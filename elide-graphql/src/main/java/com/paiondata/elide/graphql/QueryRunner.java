/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.graphql;

import com.paiondata.elide.Elide;
import com.paiondata.elide.ElideResponse;
import com.paiondata.elide.core.datastore.DataStoreTransaction;
import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.exceptions.InvalidEntityBodyException;
import com.paiondata.elide.core.request.route.Route;
import com.paiondata.elide.core.security.User;
import com.paiondata.elide.graphql.parser.GraphQLEntityProjectionMaker;
import com.paiondata.elide.graphql.parser.GraphQLProjectionInfo;
import com.paiondata.elide.graphql.parser.GraphQLQuery;
import com.paiondata.elide.graphql.parser.QueryParser;
import com.paiondata.elide.graphql.serialization.GraphQLModule;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.GraphQLError;
import graphql.GraphQLException;
import graphql.execution.AsyncSerialExecutionStrategy;
import graphql.execution.DataFetcherExceptionHandler;
import graphql.execution.SimpleDataFetcherExceptionHandler;
import graphql.validation.ValidationError;
import graphql.validation.ValidationErrorType;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Entry point for REST endpoints to execute GraphQL queries.
 */
@Slf4j
public class QueryRunner {

    @Getter
    private final Elide elide;
    private GraphQL api;

    @Getter
    private String apiVersion;

    private static final String QUERY = "query";
    private static final String OPERATION_NAME = "operationName";
    private static final String VARIABLES = "variables";
    private static final String MUTATION = "mutation";

    /**
     * Builds a new query runner.
     * @param apiVersion The API version.
     * @param elide The singular elide instance for this service.
     */
    public QueryRunner(Elide elide, String apiVersion) {
        this(elide, apiVersion, new SimpleDataFetcherExceptionHandler());
    }
    /**
     * Builds a new query runner.
     * @param elide The singular elide instance for this service.
     * @param apiVersion The API version.
     * @param exceptionHandler Overrides the default exception handler.
     */
    public QueryRunner(Elide elide, String apiVersion, DataFetcherExceptionHandler exceptionHandler) {
        this.elide = elide;
        this.apiVersion = apiVersion;

        EntityDictionary dictionary = elide.getElideSettings().getEntityDictionary();

        NonEntityDictionary nonEntityDictionary = new NonEntityDictionary(
                dictionary.getScanner(),
                dictionary.getSerdeLookup());

        PersistentResourceFetcher fetcher = new PersistentResourceFetcher(nonEntityDictionary);
        ModelBuilder builder = new ModelBuilder(elide.getElideSettings().getEntityDictionary(),
                nonEntityDictionary, elide.getElideSettings(), fetcher, apiVersion);

        this.api = GraphQL.newGraphQL(builder.build())
                .defaultDataFetcherExceptionHandler(exceptionHandler)
                .queryExecutionStrategy(new AsyncSerialExecutionStrategy(exceptionHandler))
                .build();

        elide.getElideSettings().getObjectMapper().registerModule(new GraphQLModule());
    }

    /**
     * Execute a GraphQL query and return the response.
     * @param baseUrlEndPoint base URL with prefix endpoint
     * @param graphQLDocument The graphQL document (wrapped in JSON payload).
     * @param user The user who issued the query.
     * @return The response.
     */
    public ElideResponse<String> run(String baseUrlEndPoint, String graphQLDocument, User user) {
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
    public ElideResponse<String> run(String baseUrlEndPoint, String graphQLDocument, User user, UUID requestId) {
        return run(baseUrlEndPoint, graphQLDocument, user, requestId, null);
    }

    /**
     * Execute a GraphQL query and return the response.
     * @param graphQLDocument The graphQL document (wrapped in JSON payload).
     * @param user The user who issued the query.
     * @param requestId the Request ID.
     * @return The response.
     */
    public ElideResponse<String> run(String baseUrlEndPoint, String graphQLDocument, User user, UUID requestId,
                             Map<String, List<String>> requestHeaders) {
        ObjectMapper mapper = elide.getObjectMapper();

        List<GraphQLQuery> queries;
        try {
            queries = new QueryParser() {
            }.parseDocument(graphQLDocument, mapper);
        } catch (IOException e) {
            log.debug("Invalid json body provided to GraphQL", e);
            return QueryRunner.handleRuntimeException(elide, new InvalidEntityBodyException(graphQLDocument, e));
        }

        List<ElideResponse<?>> responses = new ArrayList<>();
        for (GraphQLQuery query : queries) {
            responses.add(executeGraphQLRequest(baseUrlEndPoint, mapper, user,
                    graphQLDocument, query, requestId, requestHeaders));
        }

        if (responses.size() == 1) {
            return map(responses.get(0), elide.getObjectMapper());
        }

        //Convert the list of responses into a single JSON Array.
        ArrayNode result = responses.stream()
                .map(response -> {
                    try {
                        String body = mapper.writeValueAsString(response.getBody());
                        return mapper.readTree(body);
                    } catch (IOException e) {
                        log.debug("Caught an IO exception while trying to read response body");
                        return JsonNodeFactory.instance.objectNode();
                    }
                })
                .reduce(JsonNodeFactory.instance.arrayNode(),
                        (arrayNode, node) -> arrayNode.add(node),
                        (left, right) -> left.addAll(right));

        // Build and elide response from the array of responses.
        return map(ElideResponse.ok(result), elide.getObjectMapper());
    }

    private static ElideResponse<String> map(ElideResponse<?> response, ObjectMapper objectMapper) {
        if (response.getBody() instanceof String string) {
            return ElideResponse.status(response.getStatus()).body(string);
        } else {
            try {
                Object body = response.getBody();
                return ElideResponse.status(response.getStatus())
                        .body(body != null ? objectMapper.writeValueAsString(body) : null);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
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

    private ElideResponse<?> executeGraphQLRequest(String baseUrlEndPoint, ObjectMapper mapper, User principal,
                                                String graphQLDocument, GraphQLQuery query, UUID requestId,
                                                Map<String, List<String>> requestHeaders) {
        String queryText = query.getQuery();
        boolean isMutation = isMutation(queryText);

        try (DataStoreTransaction tx = isMutation
                ? elide.getDataStore().beginTransaction()
                : elide.getDataStore().beginReadTransaction()) {

            elide.getTransactionRegistry().addRunningTransaction(requestId, tx);
            if (query.getQuery() == null || query.getQuery().isEmpty()) {
                return ElideResponse.badRequest("A `query` key is required.");
            }

            // get variables from request for constructing entityProjections
            Map<String, Object> variables = query.getVariables();

            GraphQLProjectionInfo projectionInfo = new GraphQLEntityProjectionMaker(elide.getElideSettings(), variables,
                    apiVersion).make(queryText);
            Route route = Route.builder()
                    .baseUrl(baseUrlEndPoint)
                    .apiVersion(apiVersion)
                    .headers(requestHeaders)
                    .build();
            GraphQLRequestScope requestScope = GraphQLRequestScope.builder()
                    .route(route)
                    .dataStoreTransaction(tx)
                    .user(principal)
                    .requestId(requestId)
                    .elideSettings(elide.getElideSettings())
                    .projectionInfo(projectionInfo)
                    .build();

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
                    abortedResponseObject.put("errors", mapErrors(result.getErrors()));
                    abortedResponseObject.put("data", null);
                    // Do not commit. Throw OK response to process tx.close correctly.
                    throw new GraphQLException(mapper.writeValueAsString(abortedResponseObject));
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

            return ElideResponse.ok(result);
        } catch (IOException e) {
            return handleNonRuntimeException(elide, e, graphQLDocument);
        } catch (RuntimeException e) {
            return handleRuntimeException(elide, e);
        } finally {
            elide.getTransactionRegistry().removeRunningTransaction(requestId);
            elide.getAuditLogger().clear();
        }
    }

    /**
     * Generate more user friendly error messages.
     *
     * @param errors the errors to map
     * @return the mapped errors
     */
    private List<GraphQLError> mapErrors(List<GraphQLError> errors) {
        List<GraphQLError> result = new ArrayList<>(errors.size());
        for (GraphQLError error : errors) {
            if (error instanceof ValidationError validationError
                    && ValidationErrorType.WrongType.equals(validationError.getValidationErrorType())) {
                if (validationError.getDescription().contains("ElideRelationshipOp")) {
                    String queryPath = String.join(" ", validationError.getQueryPath());
                    RelationshipOp relationshipOp = Arrays.stream(RelationshipOp.values())
                            .filter(op -> validationError.getDescription().contains(op.name()))
                            .findFirst()
                            .orElse(null);
                    if (relationshipOp != null) {
                        result.add(ValidationError.newValidationError()
                                .description("Invalid operation: " + relationshipOp.name() + " is not permitted on "
                                        + queryPath + ".")
                                .extensions(validationError.getExtensions())
                                .validationErrorType(validationError.getValidationErrorType())
                                .sourceLocations(validationError.getLocations())
                                .queryPath(validationError.getQueryPath())
                                .build());
                        continue;
                    }
                }
            }
            result.add(error);
        }

        return result;
    }

    public static ElideResponse<String> handleNonRuntimeException(
            Elide elide,
            Exception exception,
            String graphQLDocument
    ) {
        boolean verbose = elide.getElideSettings().isVerboseErrors();
        ObjectMapper mapper = elide.getObjectMapper();
        GraphQLErrorContext errorContext = GraphQLErrorContext.builder().verbose(verbose).objectMapper(mapper)
                .graphQLDocument(graphQLDocument).build();
        GraphQLExceptionHandler exceptionHandler = elide.getSettings(GraphQLSettings.class)
                .getGraphqlExceptionHandler();
        return map(exceptionHandler.handleException(exception, errorContext), mapper);
    }

    public static ElideResponse<String> handleRuntimeException(Elide elide, RuntimeException exception) {
        boolean verbose = elide.getElideSettings().isVerboseErrors();
        ObjectMapper mapper = elide.getObjectMapper();
        GraphQLErrorContext errorContext = GraphQLErrorContext.builder().verbose(verbose).objectMapper(mapper).build();
        GraphQLExceptionHandler exceptionHandler = elide.getSettings(GraphQLSettings.class)
                .getGraphqlExceptionHandler();
        return map(exceptionHandler.handleException(exception, errorContext), mapper);
    }
}
