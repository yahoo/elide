/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.spring.controllers;

import com.yahoo.elide.Elide;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.ErrorObjects;
import com.yahoo.elide.core.exceptions.CustomErrorException;
import com.yahoo.elide.core.exceptions.HttpStatusException;
import com.yahoo.elide.core.exceptions.InvalidEntityBodyException;
import com.yahoo.elide.core.exceptions.TransactionException;
import com.yahoo.elide.graphql.GraphQLRequestScope;
import com.yahoo.elide.graphql.ModelBuilder;
import com.yahoo.elide.graphql.PersistentResourceFetcher;
import com.yahoo.elide.security.User;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import com.yahoo.elide.spring.config.ElideConfigProperties;
import org.apache.commons.lang3.tuple.Pair;
import org.owasp.encoder.Encode;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.security.Principal;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

/**
 * Spring rest controller for Elide GraphQL.
 */
@Slf4j
@RestController
@RequestMapping(value = "${elide.graphql.path}")
@EnableConfigurationProperties(ElideConfigProperties.class)
@ConditionalOnExpression("${elide.graphql.enabled:false}")
public class GraphqlController {

    private final Elide elide;
    private final ElideConfigProperties elideConfigProperties;
    private GraphQL api;

    private static final String QUERY = "query";
    private static final String OPERATION_NAME = "operationName";
    private static final String VARIABLES = "variables";
    private static final String MUTATION = "mutation";
    private static final String JSON_CONTENT_TYPE = "application/json";


    @Autowired
    public GraphqlController(Elide elide, ElideConfigProperties settings) {
        log.debug("Started ~~");

        this.elideConfigProperties = settings;
        this.elide = elide;

        PersistentResourceFetcher fetcher = new PersistentResourceFetcher(elide.getElideSettings());
        ModelBuilder builder = new ModelBuilder(elide.getElideSettings().getDictionary(), fetcher);

        this.api = new GraphQL(builder.build());

        // TODO - add serializers to allow for custom handling of ExecutionResult and GraphQLError objects
        //GraphQLErrorSerializer errorSerializer = new GraphQLErrorSerializer(elide.getElideSettings().isEncodeErrorResponses());
        //SimpleModule module = new SimpleModule("ExecutionResultSerializer", Version.unknownVersion());
        //module.addSerializer(ExecutionResult.class, new ExecutionResultSerializer(errorSerializer));
        //module.addSerializer(GraphQLError.class, errorSerializer);
        //elide.getElideSettings().getMapper().getObjectMapper().registerModule(module);
    }

    /**
     * Single entry point for GraphQL requests.
     *
     * @param graphQLDocument post data as json document
     * @param user The user principal
     * @return response
     */
    @PostMapping(value = {"/**", ""}, consumes = JSON_CONTENT_TYPE, produces = JSON_CONTENT_TYPE)
    public ResponseEntity<String> post(@RequestBody String graphQLDocument, Principal user) {
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

        Function<JsonNode, ResponseEntity<String>> executeRequest =
                (node) -> executeGraphQLRequest(mapper, user, graphQLDocument, node);

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
                            return mapper.readTree((String) response.getBody());
                        } catch (IOException e) {
                            log.debug("Caught an IO exception while trying to read response body");
                            return JsonNodeFactory.instance.objectNode();
                        }
                    })
                    .reduce(JsonNodeFactory.instance.arrayNode(),
                            (arrayNode, node) -> arrayNode.add(node),
                            (left, right) -> left.addAll(right));
            try {
                return ResponseEntity.status(HttpStatus.OK).body(mapper.writeValueAsString(result));
            } catch (IOException e) {
                log.error("An unexpected error occurred trying to serialize array response.", e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        }

        return executeRequest.apply(topLevel);
    }

    private ResponseEntity<String> executeGraphQLRequest(
            ObjectMapper mapper,
            Principal principal,
            String graphQLDocument,
            JsonNode jsonDocument) {
        boolean isVerbose = false;
        try (DataStoreTransaction tx = elide.getDataStore().beginTransaction()) {
            final User user = tx.accessUser(principal);
            GraphQLRequestScope requestScope = new GraphQLRequestScope(tx, user, elide.getElideSettings());
            isVerbose = requestScope.getPermissionExecutor().isVerbose();

            if (!jsonDocument.has(QUERY)) {
                return ResponseEntity.status(400).body("A `query` key is required.");
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

            return ResponseEntity.status(HttpStatus.OK).body(mapper.writeValueAsString(result));
        } catch (WebApplicationException e) {
            log.debug("WebApplicationException", e);
            return ResponseEntity.status(e.getResponse().getStatus()).body(e.getResponse().getEntity().toString());
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
                public Pair<Integer, JsonNode> getErrorResponse(boolean encodeResponse) {
                    return e.getErrorResponse(encodeResponse);
                }

                @Override
                public Pair<Integer, JsonNode> getVerboseErrorResponse(boolean encodeResponse) {
                    return e.getVerboseErrorResponse(encodeResponse);
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

    private ResponseEntity<String> buildErrorResponse(HttpStatusException error, boolean isVerbose) {
        ObjectMapper mapper = elide.getMapper().getObjectMapper();
        JsonNode errorNode;
        boolean encodeErrorResponses = elide.getElideSettings().isEncodeErrorResponses();
        if (!(error instanceof CustomErrorException) && elide.getElideSettings().isReturnErrorObjects()) {
            // get the error message and optionally encode it
            String errorMessage = isVerbose ? error.getVerboseMessage() : error.toString();
            if (encodeErrorResponses) {
                errorMessage = Encode.forHtml(errorMessage);
            }
            ErrorObjects errors = ErrorObjects.builder().addError()
                    .with("message", errorMessage).build();
            errorNode = mapper.convertValue(errors, JsonNode.class);
        } else {
            errorNode = isVerbose
                    ? error.getVerboseErrorResponse(encodeErrorResponses).getRight()
                    : error.getErrorResponse(encodeErrorResponses).getRight();
        }
        String errorBody;
        try {
            errorBody = mapper.writeValueAsString(errorNode);
        } catch (JsonProcessingException e) {
            errorBody = errorNode.toString();
        }
        return ResponseEntity.status(error.getStatus()).body(errorBody);
    }
}
