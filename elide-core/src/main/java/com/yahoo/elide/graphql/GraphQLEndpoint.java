/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yahoo.elide.Elide;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.RequestScope;
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
import java.util.Map;
import java.util.function.Function;

/**
 * Default endpoint/servlet for using Elide and JSONAPI.
 */
@Slf4j
@Singleton
@Produces(MediaType.APPLICATION_JSON)
@Path("/graphQLx")
public class GraphQLEndpoint {

    private Elide elide;
    private GraphQL api;
    protected final Function<SecurityContext, Object> getUser;

    @Inject
    public GraphQLEndpoint(
            @Named("elide") Elide elide,
            @Named("elideUserExtractionFunction") DefaultOpaqueUserFunction getUser) {
        log.error("Started ~~");
        this.elide = elide;
        this.getUser = getUser;
        PersistentResourceFetcher fetcher = new PersistentResourceFetcher();
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
        String path = "/";
        boolean isVerbose = false;
        try (DataStoreTransaction tx = elide.getDataStore().beginTransaction()) {
            ObjectMapper mapper = elide.getMapper().getObjectMapper();
            JsonNode jsonDocument = mapper.readTree(graphQLDocument);
            final User user = tx.accessUser(getUser.apply(securityContext));
            RequestScope requestScope = new RequestScope(path, null, tx, user, null, elide.getElideSettings());
            ExecutionResult result = api.execute(
                    jsonDocument.get("query").asText(),
                    jsonDocument.get("operationName").asText(),
                    requestScope,
                    mapper.convertValue(jsonDocument.get("variables"), Map.class));
            return Response.ok(mapper.writeValueAsString(result.getData())).build();
        } catch (JsonProcessingException e) {
            return buildErrorResponse(new InvalidEntityBodyException(graphQLDocument), isVerbose);
        } catch (IOException e) {
            return buildErrorResponse(new TransactionException(e), isVerbose);
        }
    }

    private Response buildErrorResponse(HttpStatusException error, boolean isVerbose) {
        return Response.status(error.getStatus())
                .entity(isVerbose ? error.getVerboseErrorResponse() : error.getErrorResponse()).build();
    }
}
