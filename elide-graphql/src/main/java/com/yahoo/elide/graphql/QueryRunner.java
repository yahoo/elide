/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideResponse;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.exceptions.CustomErrorException;
import com.yahoo.elide.core.exceptions.ErrorObjects;
import com.yahoo.elide.core.exceptions.ForbiddenAccessException;
import com.yahoo.elide.core.exceptions.HttpStatus;
import com.yahoo.elide.core.exceptions.HttpStatusException;
import com.yahoo.elide.core.exceptions.InvalidEntityBodyException;
import com.yahoo.elide.core.exceptions.TransactionException;
import com.yahoo.elide.core.security.User;
import com.yahoo.elide.graphql.parser.GraphQLEntityProjectionMaker;
import com.yahoo.elide.graphql.parser.GraphQLProjectionInfo;
import com.yahoo.elide.graphql.parser.GraphQLQuery;
import com.yahoo.elide.graphql.parser.QueryParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.apache.commons.lang3.tuple.Pair;
import org.owasp.encoder.Encode;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.GraphQLError;
import graphql.execution.AsyncSerialExecutionStrategy;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

/**
 * Entry point for REST endpoints to execute GraphQL queries.
 */
@Slf4j
public class QueryRunner {

    @Getter
    private final Elide elide;
    private GraphQL api;
    private ObjectMapper mapper;

    @Getter
    private String apiVersion;

    private static final String QUERY = "query";
    private static final String OPERATION_NAME = "operationName";
    private static final String VARIABLES = "variables";
    private static final String MUTATION = "mutation";

    /**
     * Builds a new query runner.
     * @param elide The singular elide instance for this service.
     */
    public QueryRunner(Elide elide, String apiVersion) {
        this.elide = elide;
        this.apiVersion = apiVersion;
        this.mapper = elide.getMapper().getObjectMapper();

        EntityDictionary dictionary = elide.getElideSettings().getDictionary();

        NonEntityDictionary nonEntityDictionary = new NonEntityDictionary(
                dictionary.getScanner(),
                dictionary.getSerdeLookup());

        PersistentResourceFetcher fetcher = new PersistentResourceFetcher(nonEntityDictionary);
        ModelBuilder builder = new ModelBuilder(elide.getElideSettings().getDictionary(),
                nonEntityDictionary, elide.getElideSettings(), fetcher, apiVersion);

        api = GraphQL.newGraphQL(builder.build())
                .queryExecutionStrategy(new AsyncSerialExecutionStrategy())
                .build();

        // TODO - add serializers to allow for custom handling of ExecutionResult and GraphQLError objects
        GraphQLErrorSerializer errorSerializer = new GraphQLErrorSerializer();
        SimpleModule module = new SimpleModule("ExecutionResultSerializer", Version.unknownVersion());
        module.addSerializer(ExecutionResult.class, new ExecutionResultSerializer(errorSerializer));
        module.addSerializer(GraphQLError.class, errorSerializer);
        elide.getElideSettings().getMapper().getObjectMapper().registerModule(module);
    }

    /**
     * Execute a GraphQL query and return the response.
     * @param baseUrlEndPoint base URL with prefix endpoint
     * @param graphQLDocument The graphQL document (wrapped in JSON payload).
     * @param user The user who issued the query.
     * @return The response.
     */
    public ElideResponse run(String baseUrlEndPoint, String graphQLDocument, User user) {
        return run(baseUrlEndPoint, graphQLDocument, user, UUID.randomUUID());
    }

    /**
     * Check if a query string is mutation.
     * @param query The graphQL Query to verify.
     * @return is a mutation.
     */
    public static boolean isMutation(String query) {
        if (query == null) {
            return false;
        }

        String[] lines = query.split("\n");

        StringBuilder withoutComments = new StringBuilder();

        for (String line : lines) {
            //Remove GraphiQL comment lines....
            if (line.matches("^(\\s*)#.*")) {
                continue;
            }
            withoutComments.append(line);
            withoutComments.append("\n");
        }

        query = withoutComments.toString().trim();

        return query.startsWith(MUTATION);
    }

    /**
     * Extracts the top level JsonNode from GraphQL document.
     * @param mapper ObjectMapper instance.
     * @param graphQLDocument The graphQL document (wrapped in JSON payload).
     * @return The JsonNode after parsing graphQLDocument.
     * @throws IOException IOException
     */
    public static JsonNode getTopLevelNode(ObjectMapper mapper, String graphQLDocument) throws IOException {
        return mapper.readTree(graphQLDocument);
    }

    /**
     * Execute a GraphQL query and return the response.
     * @param graphQLDocument The graphQL document (wrapped in JSON payload).
     * @param user The user who issued the query.
     * @param requestId the Request ID.
     * @return The response.
     */
    public ElideResponse run(String baseUrlEndPoint, String graphQLDocument, User user, UUID requestId) {
        return run(baseUrlEndPoint, graphQLDocument, user, requestId, null);
    }

    /**
     * Execute a GraphQL query and return the response.
     * @param graphQLDocument The graphQL document (wrapped in JSON payload).
     * @param user The user who issued the query.
     * @param requestId the Request ID.
     * @return The response.
     */
    public ElideResponse run(String baseUrlEndPoint, String graphQLDocument, User user, UUID requestId,
                             Map<String, List<String>> requestHeaders) {
        ObjectMapper mapper = elide.getMapper().getObjectMapper();

        List<GraphQLQuery> queries;
        try {
            queries = new QueryParser() {
            }.parseDocument(graphQLDocument, mapper);
        } catch (IOException e) {
            log.debug("Invalid json body provided to GraphQL", e);
            // NOTE: Can't get at isVerbose setting here for hardcoding to false. If necessary, we can refactor
            // so this can be set appropriately.
            return buildErrorResponse(mapper, new InvalidEntityBodyException(graphQLDocument), false);
        }

        List<ElideResponse> responses = new ArrayList<>();
        for (GraphQLQuery query : queries) {
            responses.add(executeGraphQLRequest(baseUrlEndPoint, mapper, user,
                    graphQLDocument, query, requestId, requestHeaders));
        }

        if (responses.size() == 1) {
            return responses.get(0);
        }

        //Convert the list of responses into a single JSON Array.
        ArrayNode result = responses.stream()
                .map(response -> {
                    try {
                        return mapper.readTree(response.getBody());
                    } catch (IOException e) {
                        log.debug("Caught an IO exception while trying to read response body");
                        return JsonNodeFactory.instance.objectNode();
                    }
                })
                .reduce(JsonNodeFactory.instance.arrayNode(),
                        (arrayNode, node) -> arrayNode.add(node),
                        (left, right) -> left.addAll(right));

        try {

            //Build and elide response from the array of responses.
            return ElideResponse.builder()
                    .responseCode(HttpStatus.SC_OK)
                    .body(mapper.writeValueAsString(result))
                    .build();
        } catch (IOException e) {
            log.error("An unexpected error occurred trying to serialize array response.", e);
            return ElideResponse.builder()
                    .responseCode(HttpStatus.SC_INTERNAL_SERVER_ERROR)
                    .build();
        }
    }

    /**
     * Extracts the executable query from Json Node.
     * @param jsonDocument The JsonNode object.
     * @return query to execute.
     */
    public static String extractQuery(JsonNode jsonDocument) {
        return jsonDocument.has(QUERY) ? jsonDocument.get(QUERY).asText() : null;
    }

    /**
     * Extracts the variables for the query from Json Node.
     * @param mapper ObjectMapper instance.
     * @param jsonDocument The JsonNode object.
     * @return variables to pass.
     */
    public static Map<String, Object> extractVariables(ObjectMapper mapper, JsonNode jsonDocument) {
        // get variables from request for constructing entityProjections
        Map<String, Object> variables = new HashMap<>();
        if (jsonDocument.has(VARIABLES) && !jsonDocument.get(VARIABLES).isNull()) {
            variables = mapper.convertValue(jsonDocument.get(VARIABLES), Map.class);
        }

        return variables;
    }

    /**
     * Extracts the operation name from Json Node.
     * @param jsonDocument The JsonNode object.
     * @return variables to pass.
     */
    public static String extractOperation(JsonNode jsonDocument) {
        if (jsonDocument.has(OPERATION_NAME) && !jsonDocument.get(OPERATION_NAME).isNull()) {
            return jsonDocument.get(OPERATION_NAME).asText();
        }

        return null;
    }

    private ElideResponse executeGraphQLRequest(String baseUrlEndPoint, ObjectMapper mapper, User principal,
                                                String graphQLDocument, GraphQLQuery query, UUID requestId,
                                                Map<String, List<String>> requestHeaders) {
        boolean isVerbose = false;
        String queryText = query.getQuery();
        boolean isMutation = isMutation(queryText);

        try (DataStoreTransaction tx = isMutation
                ? elide.getDataStore().beginTransaction()
                : elide.getDataStore().beginReadTransaction()) {

            elide.getTransactionRegistry().addRunningTransaction(requestId, tx);
            if (query.getQuery() == null || query.getQuery().isEmpty()) {
                return ElideResponse.builder().responseCode(HttpStatus.SC_BAD_REQUEST)
                        .body("A `query` key is required.").build();
            }

            // get variables from request for constructing entityProjections
            Map<String, Object> variables = query.getVariables();

            //TODO - get API version.
            GraphQLProjectionInfo projectionInfo = new GraphQLEntityProjectionMaker(elide.getElideSettings(), variables,
                    apiVersion).make(queryText);
            GraphQLRequestScope requestScope = new GraphQLRequestScope(baseUrlEndPoint, tx, principal, apiVersion,
                    elide.getElideSettings(), projectionInfo, requestId, requestHeaders);

            isVerbose = requestScope.getPermissionExecutor().isVerbose();

            // Logging all queries. It is recommended to put any private information that shouldn't be logged into
            // the "variables" section of your query. Variable values are not logged.
            log.info("Processing GraphQL query:\n{}", queryText);

            ExecutionInput.Builder executionInput = new ExecutionInput.Builder()
                    .localContext(requestScope)
                    .query(queryText);

            if (query.getOperationName() != null) {
                executionInput.operationName(query.getOperationName());
            }
            executionInput.variables(variables);

            ExecutionResult result = api.execute(executionInput);

            tx.preCommit(requestScope);
            requestScope.getPermissionExecutor().executeCommitChecks();
            if (isMutation) {
                if (!result.getErrors().isEmpty()) {
                    HashMap<String, Object> abortedResponseObject = new HashMap<>();
                    abortedResponseObject.put("errors", result.getErrors());
                    abortedResponseObject.put("data", null);
                    // Do not commit. Throw OK response to process tx.close correctly.
                    throw new WebApplicationException(
                            Response.ok(mapper.writeValueAsString(abortedResponseObject)).build());
                }
                requestScope.saveOrCreateObjects();
            }

            tx.flush(requestScope);

            requestScope.runQueuedPreCommitTriggers();
            elide.getAuditLogger().commit();
            tx.commit(requestScope);
            requestScope.runQueuedPostCommitTriggers();

            if (log.isTraceEnabled()) {
                requestScope.getPermissionExecutor().logCheckStats();
            }

            return ElideResponse.builder().responseCode(HttpStatus.SC_OK).body(mapper.writeValueAsString(result))
                    .build();
        } catch (IOException e) {
            return handleNonRuntimeException(elide, e, graphQLDocument, isVerbose);
        } catch (RuntimeException e) {
            return handleRuntimeException(elide, e, isVerbose);
        } finally {
            elide.getTransactionRegistry().removeRunningTransaction(requestId);
            elide.getAuditLogger().clear();
        }
    }

    public static ElideResponse handleNonRuntimeException(
            Elide elide,
            Exception error,
            String graphQLDocument,
            boolean isVerbose
    ) {
        CustomErrorException mappedException = elide.mapError(error);
        ObjectMapper mapper = elide.getMapper().getObjectMapper();

        if (mappedException != null) {
            return buildErrorResponse(mapper, mappedException, isVerbose);
        }

        if (error instanceof JsonProcessingException) {
            log.debug("Invalid json body provided to GraphQL", error);
            return buildErrorResponse(mapper, new InvalidEntityBodyException(graphQLDocument), isVerbose);
        }

        if (error instanceof IOException) {
            log.error("Uncaught IO Exception by Elide in GraphQL", error);
            return buildErrorResponse(mapper, new TransactionException(error), isVerbose);
        }

        log.error("Error or exception uncaught by Elide", error);
        throw new RuntimeException(error);
    }

    public static ElideResponse handleRuntimeException(Elide elide, RuntimeException error, boolean isVerbose) {
        CustomErrorException mappedException = elide.mapError(error);
        ObjectMapper mapper = elide.getMapper().getObjectMapper();

        if (mappedException != null) {
            return buildErrorResponse(mapper, mappedException, isVerbose);
        }

        if (error instanceof WebApplicationException) {
            WebApplicationException e = (WebApplicationException) error;
            log.debug("WebApplicationException", e);
            String body = e.getResponse().getEntity() != null ? e.getResponse().getEntity().toString() : e.getMessage();
            return ElideResponse.builder().responseCode(e.getResponse().getStatus()).body(body).build();
        }

        if (error instanceof HttpStatusException) {
            HttpStatusException e = (HttpStatusException) error;

            if (e instanceof ForbiddenAccessException) {
                if (log.isDebugEnabled()) {
                    log.debug("{}", ((ForbiddenAccessException) e).getLoggedMessage());
                }
            } else {
                log.debug("Caught HTTP status exception {}", e.getStatus(), e);
            }

            return buildErrorResponse(mapper, new HttpStatusException(200, e.getMessage()) {
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
        }

        if (error instanceof ConstraintViolationException) {
            ConstraintViolationException e = (ConstraintViolationException) error;
            log.debug("Constraint violation exception caught", e);
            String message = "Constraint violation";
            final ErrorObjects.ErrorObjectsBuilder errorObjectsBuilder = ErrorObjects.builder();
            for (ConstraintViolation<?> constraintViolation : e.getConstraintViolations()) {
                errorObjectsBuilder.addError()
                        .withDetail(constraintViolation.getMessage());
                final String propertyPathString = constraintViolation.getPropertyPath().toString();
                if (!propertyPathString.isEmpty()) {
                    Map<String, Object> source = new HashMap<>(1);
                    source.put("property", propertyPathString);
                    errorObjectsBuilder.with("source", source);
                }
            }
            return buildErrorResponse(
                    mapper,
                    new CustomErrorException(HttpStatus.SC_OK, message, errorObjectsBuilder.build()),
                    isVerbose
            );
        }

        log.error("Error or exception uncaught by Elide", error);
        throw new RuntimeException(error);
    }

    public static ElideResponse buildErrorResponse(ObjectMapper mapper, HttpStatusException error, boolean isVerbose) {
        JsonNode errorNode;
        if (!(error instanceof CustomErrorException)) {
            // get the error message and optionally encode it
            String errorMessage = isVerbose ? error.getVerboseMessage() : error.getMessage();
            errorMessage = Encode.forHtml(errorMessage);
            ErrorObjects errors = ErrorObjects.builder().addError()
                    .with("message", errorMessage).build();
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
        return ElideResponse.builder()
                .responseCode(error.getStatus())
                .body(errorBody)
                .build();
    }
}
