/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yahoo.elide.Elide;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.exceptions.HttpStatusException;
import com.yahoo.elide.core.exceptions.InvalidEntityBodyException;
import com.yahoo.elide.core.exceptions.TransactionException;
import com.yahoo.elide.resources.DefaultOpaqueUserFunction;
import com.yahoo.elide.security.User;
import graphql.ExecutionResult;
import graphql.GraphQL;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Default endpoint/servlet for using Elide and JSONAPI.
 */
@Slf4j
@Singleton
@Produces(MediaType.APPLICATION_JSON)
@Path("/")
public class GraphQLEndpoint {

    private Elide elide;
    private GraphQL api;
    protected final Function<SecurityContext, Object> getUser;

    private static final String QUERY = "query";
    private static final String OPERATION_NAME = "operationName";
    private static final String VARIABLES = "variables";
    private static final String MUTATION = "mutation";

    @Inject
    public GraphQLEndpoint(
            @Named("elide") Elide elide,
            @Named("elideUserExtractionFunction") DefaultOpaqueUserFunction getUser) {
        log.error("Started ~~");
        this.elide = elide;
        this.getUser = getUser;
        PersistentResourceFetcher fetcher = new PersistentResourceFetcher(elide.getElideSettings());
        ModelBuilder builder = new ModelBuilder(elide.getElideSettings().getDictionary(), fetcher);
        this.api = new GraphQL(builder.build());
    }

    /**
     * Create handler.
     *
     * @param securityContext security context
     * @param graphQLDocument post data as jsonapi document
     * @return response
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response post(
            @Context SecurityContext securityContext,
            String graphQLDocument) {
        boolean isVerbose = false;
        try (DataStoreTransaction tx = elide.getDataStore().beginTransaction()) {
            ObjectMapper mapper = elide.getMapper().getObjectMapper();
            JsonNode jsonDocument = mapper.readTree(graphQLDocument);
            final User user = tx.accessUser(getUser.apply(securityContext));
            GraphQLRequestScope requestScope = new GraphQLRequestScope(tx, user, elide.getElideSettings());
            isVerbose = requestScope.getPermissionExecutor().isVerbose();

            if (!jsonDocument.has(QUERY)) {
                return Response.status(400).entity("A `query` key is required.").build();
            }

            String query = jsonDocument.get(QUERY).asText();

            String operationName = null;
            if (jsonDocument.has(OPERATION_NAME)) {
                operationName = jsonDocument.get(OPERATION_NAME).asText();
            }

            ExecutionResult result;
            if (jsonDocument.has(VARIABLES)) {
                Map<String, Object> variables = mapper.convertValue(jsonDocument.get(VARIABLES), Map.class);
                result = api.execute(query, operationName, requestScope, variables);
            } else {
                result = api.execute(query, operationName, requestScope);
            }

            tx.preCommit();
            requestScope.runQueuedPreSecurityTriggers();
            requestScope.getPermissionExecutor().executeCommitChecks();
            if (query.trim().startsWith(MUTATION)) {
                if (!result.getErrors().isEmpty()) {
                    HashMap<String, Object> abortedResponseObject = new HashMap<String, Object>() {
                        {
                            put("errors", result.getErrors());
                            put("data", new HashMap<>());
                        }
                    };
                    // Do not commit.
                    return Response.ok(mapper.writeValueAsString(abortedResponseObject)).build();
                }
                requestScope.saveOrCreateObjects();
            }

            requestScope.runQueuedPreCommitTriggers();
            elide.getAuditLogger().commit(requestScope);
            tx.commit(requestScope);
            requestScope.runQueuedPostCommitTriggers();

            if (log.isTraceEnabled()) {
                requestScope.getPermissionExecutor().printCheckStats();
            }

            return Response.ok(mapper.writeValueAsString(result)).build();
        } catch (JsonProcessingException e) {
            return buildErrorResponse(new InvalidEntityBodyException(graphQLDocument), isVerbose);
        } catch (IOException e) {
            return buildErrorResponse(new TransactionException(e), isVerbose);
        } catch (Exception | Error e) {
            log.debug("Unhandled error or exception.", e);
            throw e;
        } finally {
            elide.getAuditLogger().clear();
        }
    }

    private Response buildErrorResponse(HttpStatusException error, boolean isVerbose) {
        return Response.status(error.getStatus())
                .entity(isVerbose ? error.getVerboseErrorResponse() : error.getErrorResponse()).build();
    }
}
