/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.ErrorObjects;
import com.yahoo.elide.core.exceptions.CustomErrorException;
import com.yahoo.elide.core.exceptions.HttpStatusException;
import com.yahoo.elide.core.exceptions.InvalidEntityBodyException;
import com.yahoo.elide.core.exceptions.TransactionException;
import com.yahoo.elide.resources.DefaultOpaqueUserFunction;
import com.yahoo.elide.security.User;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import org.apache.commons.lang3.tuple.Pair;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

/**
 * Default endpoint/servlet for using Elide and JSONAPI.
 */
@Slf4j
@Singleton
@Produces(MediaType.APPLICATION_JSON)
@Path("/")
public class GraphQLEndpoint {

    private Elide elide;
    private ElideSettings elideSettings;
    private GraphQL api;
    protected final Function<SecurityContext, Object> getUser;

    private static final String QUERY = "query";
    private static final String OPERATION_NAME = "operationName";
    private static final String VARIABLES = "variables";
    private static final String MUTATION = "mutation";
    private static final DefaultOpaqueUserFunction DEFAULT_GET_USER = securityContext -> securityContext;

    @Inject
    public GraphQLEndpoint(
            @Named("elide") Elide elide,
            @Named("elideUserExtractionFunction") DefaultOpaqueUserFunction getUser) {
        log.error("Started ~~");
        this.elide = elide;
        this.elideSettings = elide.getElideSettings();
        this.getUser = getUser == null ? DEFAULT_GET_USER : getUser;
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
        ObjectMapper mapper = elide.getMapper().getObjectMapper();

        JsonNode topLevel;

        try {
             topLevel = mapper.readTree(graphQLDocument);
        } catch (IOException e) {
            log.debug("Invalid json body provided to GraphQL", e);
            // NOTE: Can't get at isVerbose setting here for hardcoding to false. If necessary, we can refactor
            // so this can be set appropriately.
            return buildErrorResponse(new InvalidEntityBodyException(graphQLDocument), false);
        }

        Function<JsonNode, Response> executeRequest =
                (node) -> executeGraphQLRequest(mapper, securityContext, graphQLDocument, node);

        if (topLevel.isArray()) {
            Iterator<JsonNode> nodeIterator = topLevel.iterator();
            Iterable<JsonNode> nodeIterable = () -> nodeIterator;
            // NOTE: Create a non-parallel stream
            // It's unclear whether or not the expectations of the caller would be that requests are intended
            // to run serially even outside of a single transaction. We should revisit this.
            Stream<JsonNode> nodeStream = StreamSupport.stream(nodeIterable.spliterator(), false);
            ArrayNode result = nodeStream
                    .map(executeRequest)
                    .map(response -> {
                        try {
                            return mapper.readTree((String) response.getEntity());
                        } catch (IOException e) {
                            log.debug("Caught an IO exception while trying to read response body");
                            return JsonNodeFactory.instance.objectNode();
                        }
                    })
                    .reduce(JsonNodeFactory.instance.arrayNode(),
                            (arrayNode, node) -> arrayNode.add(node),
                            (left, right) -> left.addAll(right));
            try {
                return Response.ok(mapper.writeValueAsString(result)).build();
            } catch (IOException e) {
                log.error("An unexpected error occurred trying to serialize array response.", e);
                return Response.serverError().build();
            }
        }

        return executeRequest.apply(topLevel);
    }

    private Response executeGraphQLRequest(
            ObjectMapper mapper,
            SecurityContext securityContext,
            String graphQLDocument,
            JsonNode jsonDocument) {
        boolean isVerbose = false;
        try (DataStoreTransaction tx = elide.getDataStore().beginTransaction()) {
            final User user = tx.accessUser(getUser.apply(securityContext));
            GraphQLRequestScope requestScope = new GraphQLRequestScope(tx, user, elide.getElideSettings());
            isVerbose = requestScope.getPermissionExecutor().isVerbose();

            if (!jsonDocument.has(QUERY)) {
                return Response.status(400).entity("A `query` key is required.").build();
            }

            String query = jsonDocument.get(QUERY).asText();

            // Logging all queries. It is recommended to put any private information that shouldn't be logged into
            // the "variables" section of your query. Variable values are not logged.
            log.info("Processing GraphQL query:\n{}", query);

            ExecutionInput.Builder executionInput = new ExecutionInput.Builder()
                    .context(requestScope)
                    .query(query);

            if (jsonDocument.has(OPERATION_NAME) && !jsonDocument.get(OPERATION_NAME).isNull()) {
                executionInput.operationName(jsonDocument.get(OPERATION_NAME).asText());
            }

            if (jsonDocument.has(VARIABLES) && !jsonDocument.get(VARIABLES).isNull()) {
                Map<String, Object> variables = mapper.convertValue(jsonDocument.get(VARIABLES), Map.class);
                executionInput.variables(variables);
            }

            ExecutionResult result = api.execute(executionInput);

            tx.preCommit();
            requestScope.runQueuedPreSecurityTriggers();
            requestScope.getPermissionExecutor().executeCommitChecks();
            if (query.trim().startsWith(MUTATION)) {
                if (!result.getErrors().isEmpty()) {
                    HashMap<String, Object> abortedResponseObject = new HashMap<String, Object>() {
                        {
                            put("errors", result.getErrors());
                            put("data", null);
                        }
                    };
                    // Do not commit. Throw OK response to process tx.close correctly.
                    throw new WebApplicationException(
                            Response.ok(mapper.writeValueAsString(abortedResponseObject)).build());
                }
                requestScope.saveOrCreateObjects();
            }
            tx.flush(requestScope);

            requestScope.runQueuedPreCommitTriggers();
            elide.getAuditLogger().commit(requestScope);
            tx.commit(requestScope);
            requestScope.runQueuedPostCommitTriggers();

            if (log.isTraceEnabled()) {
                requestScope.getPermissionExecutor().printCheckStats();
            }

            return Response.ok(mapper.writeValueAsString(result.toSpecification())).build();
        } catch (WebApplicationException e) {
            log.debug("WebApplicationException", e);
            return e.getResponse();
        } catch (JsonProcessingException e) {
            log.debug("Invalid json body provided to GraphQL", e);
            return buildErrorResponse(new InvalidEntityBodyException(graphQLDocument), isVerbose);
        } catch (IOException e) {
            log.error("Uncaught IO Exception by Elide in GraphQL", e);
            return buildErrorResponse(new TransactionException(e), isVerbose);
        } catch (HttpStatusException e) {
            log.debug("Caught HTTP status exception {}", e.getStatus(), e);
            return buildErrorResponse(new HttpStatusException(200, "") {
                @Override
                public int getStatus() {
                    return 200;
                }

                @Override
                public Pair<Integer, JsonNode> getErrorResponse() {
                    return e.getErrorResponse();
                }

                @Override
                public Pair<Integer, JsonNode> getVerboseErrorResponse() {
                    return e.getVerboseErrorResponse();
                }

                @Override
                public String getVerboseMessage() {
                    return e.getVerboseMessage();
                }

                @Override
                public String toString() {
                    return e.toString();
                }
            }, isVerbose);
        } catch (Exception | Error e) {
            log.debug("Unhandled error or exception.", e);
            throw e;
        } finally {
            elide.getAuditLogger().clear();
        }
    }

    private Response buildErrorResponse(HttpStatusException error, boolean isVerbose) {
        ObjectMapper mapper = elide.getMapper().getObjectMapper();
        JsonNode errorNode;
        if (!(error instanceof CustomErrorException) && elideSettings.isReturnErrorObjects()) {
            ErrorObjects errors = ErrorObjects.builder().addError()
                    .with("message", isVerbose ? error.getVerboseMessage() : error.toString()).build();
            errorNode = mapper.convertValue(errors, JsonNode.class);
        } else {
            errorNode = isVerbose
                    ? error.getVerboseErrorResponse().getRight()
                    : error.getErrorResponse().getRight();
        }
        String errorBody;
        try {
            errorBody = mapper.writeValueAsString(errorNode);
        } catch (JsonProcessingException e) {
            errorBody = errorNode.toString();
        }
        return Response.status(error.getStatus())
                .entity(errorBody)
                .build();
    }
}
