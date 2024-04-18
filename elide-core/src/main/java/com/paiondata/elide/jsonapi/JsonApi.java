/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.jsonapi;

import com.paiondata.elide.Elide;
import com.paiondata.elide.ElideResponse;
import com.paiondata.elide.ElideSettings;
import com.paiondata.elide.RefreshableElide;
import com.paiondata.elide.core.TransactionRegistry;
import com.paiondata.elide.core.audit.AuditLogger;
import com.paiondata.elide.core.datastore.DataStore;
import com.paiondata.elide.core.datastore.DataStoreTransaction;
import com.paiondata.elide.core.exceptions.BadRequestException;
import com.paiondata.elide.core.exceptions.HttpStatus;
import com.paiondata.elide.core.request.route.Route;
import com.paiondata.elide.core.security.User;
import com.paiondata.elide.jsonapi.extensions.JsonApiAtomicOperations;
import com.paiondata.elide.jsonapi.extensions.JsonApiAtomicOperationsRequestScope;
import com.paiondata.elide.jsonapi.extensions.JsonApiJsonPatch;
import com.paiondata.elide.jsonapi.extensions.JsonApiJsonPatchRequestScope;
import com.paiondata.elide.jsonapi.models.JsonApiDocument;
import com.paiondata.elide.jsonapi.parser.BaseVisitor;
import com.paiondata.elide.jsonapi.parser.DeleteVisitor;
import com.paiondata.elide.jsonapi.parser.GetVisitor;
import com.paiondata.elide.jsonapi.parser.JsonApiParser;
import com.paiondata.elide.jsonapi.parser.PatchVisitor;
import com.paiondata.elide.jsonapi.parser.PostVisitor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * JSON:API.
 */
@Slf4j
public class JsonApi {
    @Getter
    private final Elide elide;

    private final ElideSettings elideSettings;
    private final JsonApiSettings jsonApiSettings;
    private final DataStore dataStore;
    private final JsonApiMapper mapper;
    private final TransactionRegistry transactionRegistry;
    private final AuditLogger auditLogger;
    private final JsonApiExceptionHandler jsonApiExceptionHandler;
    private final boolean strictQueryParameters;

    public JsonApi(RefreshableElide refreshableElide) {
        this(refreshableElide.getElide());
    }

    public JsonApi(Elide elide) {
        this.elide = elide;
        this.jsonApiSettings = elide.getSettings(JsonApiSettings.class);
        this.strictQueryParameters = this.jsonApiSettings.isStrictQueryParameters();
        this.mapper = this.jsonApiSettings.getJsonApiMapper();
        this.dataStore = this.elide.getDataStore();
        this.elideSettings = this.elide.getElideSettings();
        this.transactionRegistry = this.elide.getTransactionRegistry();
        this.auditLogger = this.elide.getAuditLogger();
        this.jsonApiExceptionHandler = this.jsonApiSettings.getJsonApiExceptionHandler();
    }

    /**
     * Handle GET.
     *
     * @param route the route
     * @param opaqueUser the opaque user
     * @param requestId the request ID
     * @return Elide response object
     */
    public ElideResponse<String> get(Route route, User opaqueUser,
                             UUID requestId) {
        UUID requestUuid = requestId != null ? requestId : UUID.randomUUID();

        if (strictQueryParameters) {
            try {
                verifyQueryParams(route.getParameters());
            } catch (BadRequestException e) {
                JsonApiErrorContext errorContext = JsonApiErrorContext.builder().mapper(this.mapper).verbose(false)
                        .build();
                ElideResponse<?> errorResponse = jsonApiExceptionHandler.handleException(e, errorContext);
                return toResponse(errorResponse.getStatus(), errorResponse.getBody());
            }
        }
        return handleRequest(true, opaqueUser, dataStore::beginReadTransaction, requestUuid, (tx, user) -> {
            JsonApiDocument jsonApiDoc = new JsonApiDocument();
            JsonApiRequestScope requestScope = JsonApiRequestScope.builder().route(route).dataStoreTransaction(tx)
                    .user(user).requestId(requestUuid).elideSettings(elideSettings).jsonApiDocument(jsonApiDoc)
                    .build();
            requestScope.setEntityProjection(new EntityProjectionMaker(elideSettings.getEntityDictionary(),
                    requestScope).parsePath(route.getPath()));
            BaseVisitor visitor = new GetVisitor(requestScope);
            return visit(route.getPath(), requestScope, visitor);
        });
    }

    /**
     * Handle POST.
     *
     * @param route the route
     * @param jsonApiDocument the json api document
     * @param opaqueUser the opaque user
     * @param requestId the request ID
     * @return Elide response object
     */
    public ElideResponse<String> post(Route route, String jsonApiDocument,
                              User opaqueUser, UUID requestId) {
        UUID requestUuid = requestId != null ? requestId : UUID.randomUUID();

        return handleRequest(false, opaqueUser, dataStore::beginTransaction, requestUuid, (tx, user) -> {
            JsonApiDocument jsonApiDoc = mapper.readJsonApiDocument(jsonApiDocument);
            JsonApiRequestScope requestScope = JsonApiRequestScope.builder().route(route).dataStoreTransaction(tx)
                    .user(user).requestId(requestUuid).elideSettings(elideSettings).jsonApiDocument(jsonApiDoc)
                    .build();
            requestScope.setEntityProjection(new EntityProjectionMaker(elideSettings.getEntityDictionary(),
                    requestScope).parsePath(route.getPath()));
            BaseVisitor visitor = new PostVisitor(requestScope);
            return visit(route.getPath(), requestScope, visitor);
        });
    }

    /**
     * Handle PATCH.
     *
     * @param route the route
     * @param jsonApiDocument the json api document
     * @param opaqueUser the opaque user
     * @param requestId the request ID
     * @return Elide response object
     */
    public ElideResponse<String> patch(Route route, String jsonApiDocument, User opaqueUser, UUID requestId) {
        UUID requestUuid = requestId != null ? requestId : UUID.randomUUID();

        String accept = getFirstHeaderValueOrEmpty(route, "accept");
        String contentType = getFirstHeaderValueOrEmpty(route, "content-type");

        Handler<DataStoreTransaction, User, HandlerResult> handler;
        if (JsonApiJsonPatch.isPatchExtension(contentType) && JsonApiJsonPatch.isPatchExtension(accept)) {
            handler = (tx, user) -> {
                JsonApiJsonPatchRequestScope requestScope = new JsonApiJsonPatchRequestScope(route, tx, user,
                        requestUuid, elideSettings);
                try {
                    Supplier<Pair<Integer, JsonNode>> responder = JsonApiJsonPatch.processJsonPatch(dataStore,
                            route.getPath(), jsonApiDocument, requestScope);
                    return new HandlerResult(requestScope, responder);
                } catch (RuntimeException e) {
                    return new HandlerResult(requestScope, e);
                }
            };
        } else {
            handler = (tx, user) -> {
                JsonApiDocument jsonApiDoc = mapper.readJsonApiDocument(jsonApiDocument);
                JsonApiRequestScope requestScope = JsonApiRequestScope.builder().route(route).dataStoreTransaction(tx)
                        .user(user).requestId(requestUuid).elideSettings(elideSettings).jsonApiDocument(jsonApiDoc)
                        .build();
                requestScope.setEntityProjection(new EntityProjectionMaker(elideSettings.getEntityDictionary(),
                        requestScope).parsePath(route.getPath()));
                BaseVisitor visitor = new PatchVisitor(requestScope);
                return visit(route.getPath(), requestScope, visitor);
            };
        }

        return handleRequest(false, opaqueUser, dataStore::beginTransaction, requestUuid, handler);
    }

    /**
     * Handle DELETE.
     *
     * @param route the route
     * @param jsonApiDocument the json api document
     * @param opaqueUser the opaque user
     * @param requestId the request ID
     * @return Elide response object
     */
    public ElideResponse<String> delete(Route route, String jsonApiDocument,
                                User opaqueUser, UUID requestId) {
        UUID requestUuid = requestId != null ? requestId : UUID.randomUUID();

        return handleRequest(false, opaqueUser, dataStore::beginTransaction, requestUuid, (tx, user) -> {
            JsonApiDocument jsonApiDoc = StringUtils.isEmpty(jsonApiDocument)
                    ? new JsonApiDocument()
                    : mapper.readJsonApiDocument(jsonApiDocument);
            JsonApiRequestScope requestScope = JsonApiRequestScope.builder().route(route).dataStoreTransaction(tx)
                    .user(user).requestId(requestUuid).elideSettings(elideSettings).jsonApiDocument(jsonApiDoc).build();
            requestScope.setEntityProjection(new EntityProjectionMaker(elideSettings.getEntityDictionary(),
                    requestScope).parsePath(route.getPath()));
            BaseVisitor visitor = new DeleteVisitor(requestScope);
            return visit(route.getPath(), requestScope, visitor);
        });
    }

    /**
     * Handle operations for the Atomic Operations extension.
     *
     * @param route the route
     * @param jsonApiDocument the json api document
     * @param opaqueUser the opaque user
     * @param requestId the request ID
     * @return Elide response object
     */
    public ElideResponse<String> operations(Route route,
            String jsonApiDocument, User opaqueUser, UUID requestId) {

        UUID requestUuid = requestId != null ? requestId : UUID.randomUUID();

        String accept = getFirstHeaderValueOrEmpty(route, "accept");
        String contentType = getFirstHeaderValueOrEmpty(route, "content-type");

        Handler<DataStoreTransaction, User, HandlerResult> handler;
        if (JsonApiAtomicOperations.isAtomicOperationsExtension(contentType)
                && JsonApiAtomicOperations.isAtomicOperationsExtension(accept)) {
            handler = (tx, user) -> {
                JsonApiAtomicOperationsRequestScope requestScope = new JsonApiAtomicOperationsRequestScope(
                        route, tx, user, requestUuid, elideSettings);
                try {
                    Supplier<Pair<Integer, JsonNode>> responder = JsonApiAtomicOperations
                            .processAtomicOperations(dataStore, route.getPath(), jsonApiDocument, requestScope);
                    return new HandlerResult(requestScope, responder);
                } catch (RuntimeException e) {
                    return new HandlerResult(requestScope, e);
                }
            };
        } else {
            return ElideResponse.status(HttpStatus.SC_UNSUPPORTED_MEDIA_TYPE).body("Unsupported Media Type");
        }

        return handleRequest(false, opaqueUser, dataStore::beginTransaction, requestUuid, handler);
    }

    public HandlerResult visit(String path, JsonApiRequestScope requestScope, BaseVisitor visitor) {
        try {
            Supplier<Pair<Integer, JsonApiDocument>> responder = visitor.visit(JsonApiParser.parse(path));
            return new HandlerResult(requestScope, responder);
        } catch (RuntimeException e) {
            return new HandlerResult(requestScope, e);
        }
    }

    /**
     * Handle JSON API requests.
     *
     * @param isReadOnly if the transaction is read only
     * @param user the user object from the container
     * @param transaction a transaction supplier
     * @param requestId the Request ID
     * @param handler a function that creates the request scope and request handler
     * @param <T> The response type (JsonNode or JsonApiDocument)
     * @return the response
     */
    protected <T> ElideResponse<String> handleRequest(boolean isReadOnly, User user,
                                          Supplier<DataStoreTransaction> transaction, UUID requestId,
                                          Handler<DataStoreTransaction, User, HandlerResult> handler) {
        JsonApiErrorContext errorContext = JsonApiErrorContext.builder().mapper(this.mapper)
                .verbose(elideSettings.isVerboseErrors()).build();
        try (DataStoreTransaction tx = transaction.get()) {
            transactionRegistry.addRunningTransaction(requestId, tx);
            HandlerResult result = handler.handle(tx, user);
            JsonApiRequestScope requestScope = result.getRequestScope();
            Supplier<Pair<Integer, T>> responder = result.getResponder();
            tx.preCommit(requestScope);
            requestScope.runQueuedPreSecurityTriggers();
            requestScope.getPermissionExecutor().executeCommitChecks();
            requestScope.runQueuedPreFlushTriggers();
            if (!isReadOnly) {
                requestScope.saveOrCreateObjects();
            }
            tx.flush(requestScope);

            requestScope.runQueuedPreCommitTriggers();

            ElideResponse<String> response = buildResponse(responder.get());

            auditLogger.commit();
            tx.commit(requestScope);
            requestScope.runQueuedPostCommitTriggers();

            if (log.isTraceEnabled()) {
                requestScope.getPermissionExecutor().logCheckStats();
            }

            return response;
        } catch (Throwable e) {
            ElideResponse<?> errorResponse = jsonApiExceptionHandler.handleException(e, errorContext);
            return toResponse(errorResponse.getStatus(), errorResponse.getBody());
        } finally {
            transactionRegistry.removeRunningTransaction(requestId);
            auditLogger.clear();
        }
    }

    protected ElideResponse<String> toResponse(int status, Object body) {
        String result = null;
        if (body instanceof String data) {
            result = data;
        } else {
            try {
                result = body != null ? this.mapper.writeJsonApiDocument(body) : null;
            } catch (JsonProcessingException e) {
                return ElideResponse.status(HttpStatus.SC_INTERNAL_SERVER_ERROR).body(e.toString());
            }
        }
        return ElideResponse.status(status).body(result);
    }

    protected <T> ElideResponse<String> buildResponse(Pair<Integer, T> response) {
        T responseNode = response.getRight();
        Integer responseCode = response.getLeft();
        return toResponse(responseCode, responseNode);
    }

    private void verifyQueryParams(Map<String, List<String>> queryParams) {
        String undefinedKeys = queryParams.keySet()
                        .stream()
                        .filter(JsonApi::notAValidKey)
                        .collect(Collectors.joining(", "));

        if (!undefinedKeys.isEmpty()) {
            throw new BadRequestException("Found undefined keys in request: " + undefinedKeys);
        }
    }

    private static boolean notAValidKey(String key) {
        boolean validKey = key.equals("sort")
                        || key.startsWith("filter")
                        || (key.startsWith("fields[") && key.endsWith("]"))
                        || key.startsWith("page[")
                        || key.equals(EntityProjectionMaker.INCLUDE);
        return !validKey;
    }

    private static String getFirstHeaderValueOrEmpty(Route route, String headerName) {
        return route.getHeaders().getOrDefault(headerName, Collections.emptyList()).stream().findFirst().orElse("");
    }

    /**
     * A function that sets up the request handling objects.
     *
     * @param <DataStoreTransaction> the request's transaction
     * @param <User> the request's user
     * @param <HandlerResult> the request handling objects
     */
    @FunctionalInterface
    public interface Handler<DataStoreTransaction, User, HandlerResult> {
        HandlerResult handle(DataStoreTransaction a, User b) throws IOException;
    }

    /**
     * A wrapper to return multiple values, less verbose than Pair.
     * @param <T> Response type.
     */
    protected static class HandlerResult<T> {
        protected JsonApiRequestScope requestScope;
        protected Supplier<Pair<Integer, T>> result;
        protected RuntimeException cause;

        protected HandlerResult(JsonApiRequestScope requestScope, Supplier<Pair<Integer, T>> result) {
            this.requestScope = requestScope;
            this.result = result;
        }

        public HandlerResult(JsonApiRequestScope requestScope, RuntimeException cause) {
            this.requestScope = requestScope;
            this.cause = cause;
        }

        public Supplier<Pair<Integer, T>> getResponder() {
            if (cause != null) {
                throw cause;
            }
            return result;
        }

        public JsonApiRequestScope getRequestScope() {
            return requestScope;
        }
    }

    public static final String MEDIA_TYPE = "application/vnd.api+json";

    public static class JsonPatch {
        private JsonPatch() {
        }

        public static final String MEDIA_TYPE = "application/vnd.api+json; ext=jsonpatch";
    }

    public static class AtomicOperations {
        private AtomicOperations() {
        }

        public static final String MEDIA_TYPE = "application/vnd.api+json; ext=\"https://jsonapi.org/ext/atomic\"";
    }
}
