/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.jsonapi;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideResponse;
import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.RefreshableElide;
import com.yahoo.elide.core.TransactionRegistry;
import com.yahoo.elide.core.audit.AuditLogger;
import com.yahoo.elide.core.datastore.DataStore;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.exceptions.BadRequestException;
import com.yahoo.elide.core.exceptions.CustomErrorException;
import com.yahoo.elide.core.exceptions.ErrorObjects;
import com.yahoo.elide.core.exceptions.ForbiddenAccessException;
import com.yahoo.elide.core.exceptions.HttpStatus;
import com.yahoo.elide.core.exceptions.HttpStatusException;
import com.yahoo.elide.core.exceptions.InternalServerErrorException;
import com.yahoo.elide.core.exceptions.InvalidURLException;
import com.yahoo.elide.core.exceptions.JsonApiAtomicOperationsException;
import com.yahoo.elide.core.exceptions.JsonPatchExtensionException;
import com.yahoo.elide.core.exceptions.TransactionException;
import com.yahoo.elide.core.request.route.Route;
import com.yahoo.elide.core.security.User;
import com.yahoo.elide.jsonapi.extensions.JsonApiAtomicOperations;
import com.yahoo.elide.jsonapi.extensions.JsonApiAtomicOperationsRequestScope;
import com.yahoo.elide.jsonapi.extensions.JsonApiJsonPatch;
import com.yahoo.elide.jsonapi.extensions.JsonApiJsonPatchRequestScope;
import com.yahoo.elide.jsonapi.models.JsonApiDocument;
import com.yahoo.elide.jsonapi.parser.BaseVisitor;
import com.yahoo.elide.jsonapi.parser.DeleteVisitor;
import com.yahoo.elide.jsonapi.parser.GetVisitor;
import com.yahoo.elide.jsonapi.parser.JsonApiParser;
import com.yahoo.elide.jsonapi.parser.PatchVisitor;
import com.yahoo.elide.jsonapi.parser.PostVisitor;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.WebApplicationException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.HashMap;
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
    private final DataStore dataStore;
    private final JsonApiMapper mapper;
    private final TransactionRegistry transactionRegistry;
    private final AuditLogger auditLogger;

    public JsonApi(RefreshableElide refreshableElide) {
        this(refreshableElide.getElide());
    }

    public JsonApi(Elide elide) {
        this.elide = elide;
        this.mapper = this.elide.getMapper();
        this.dataStore = this.elide.getDataStore();
        this.elideSettings = this.elide.getElideSettings();
        this.transactionRegistry = this.elide.getTransactionRegistry();
        this.auditLogger = this.elide.getAuditLogger();
    }

    /**
     * Handle GET.
     *
     * @param baseUrlEndPoint base URL with prefix endpoint
     * @param path the path
     * @param queryParams the query params
     * @param requestHeaders the request headers
     * @param opaqueUser the opaque user
     * @param apiVersion the API version
     * @param requestId the request ID
     * @return Elide response object
     */
    public ElideResponse get(Route route, User opaqueUser,
                             UUID requestId) {
        UUID requestUuid = requestId != null ? requestId : UUID.randomUUID();

        if (elideSettings.isStrictQueryParams()) {
            try {
                verifyQueryParams(route.getParameters());
            } catch (BadRequestException e) {
                return buildErrorResponse(e, false);
            }
        }
        return handleRequest(true, opaqueUser, dataStore::beginReadTransaction, requestUuid, (tx, user) -> {
            JsonApiDocument jsonApiDoc = new JsonApiDocument();
            JsonApiRequestScope requestScope = new JsonApiRequestScope(route, tx, user, requestUuid, elideSettings,
                    jsonApiDoc);
            requestScope.setEntityProjection(new EntityProjectionMaker(elideSettings.getDictionary(),
                    requestScope).parsePath(route.getPath()));
            BaseVisitor visitor = new GetVisitor(requestScope);
            return visit(route.getPath(), requestScope, visitor);
        });
    }


    /**
     * Handle POST.
     *
     * @param baseUrlEndPoint base URL with prefix endpoint
     * @param path the path
     * @param jsonApiDocument the json api document
     * @param queryParams the query params
     * @param requestHeaders the request headers
     * @param opaqueUser the opaque user
     * @param apiVersion the API version
     * @param requestId the request ID
     * @return Elide response object
     */
    public ElideResponse post(Route route, String jsonApiDocument,
                              User opaqueUser, UUID requestId) {
        UUID requestUuid = requestId != null ? requestId : UUID.randomUUID();

        return handleRequest(false, opaqueUser, dataStore::beginTransaction, requestUuid, (tx, user) -> {
            JsonApiDocument jsonApiDoc = mapper.readJsonApiDocument(jsonApiDocument);
            JsonApiRequestScope requestScope = new JsonApiRequestScope(route, tx, user, requestUuid, elideSettings,
                    jsonApiDoc);
            requestScope.setEntityProjection(new EntityProjectionMaker(elideSettings.getDictionary(),
                    requestScope).parsePath(route.getPath()));
            BaseVisitor visitor = new PostVisitor(requestScope);
            return visit(route.getPath(), requestScope, visitor);
        });
    }


    /**
     * Handle PATCH.
     *
     * @param baseUrlEndPoint base URL with prefix endpoint
     * @param contentType the content type
     * @param accept the accept
     * @param path the path
     * @param jsonApiDocument the json api document
     * @param queryParams the query params
     * @param requestHeaders the request headers
     * @param opaqueUser the opaque user
     * @param apiVersion the API version
     * @param requestId the request ID
     * @return Elide response object
     */
    public ElideResponse patch(Route route, String jsonApiDocument, User opaqueUser, UUID requestId) {
        UUID requestUuid = requestId != null ? requestId : UUID.randomUUID();

        String accept = route.getHeaders().get("accept").stream().findFirst().orElse("");
        String contentType = route.getHeaders().get("content-type").stream().findFirst().orElse("");

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
                JsonApiRequestScope requestScope = new JsonApiRequestScope(route, tx, user, requestUuid, elideSettings,
                        jsonApiDoc);
                requestScope.setEntityProjection(new EntityProjectionMaker(elideSettings.getDictionary(),
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
     * @param baseUrlEndPoint base URL with prefix endpoint
     * @param path the path
     * @param jsonApiDocument the json api document
     * @param queryParams the query params
     * @param requestHeaders the request headers
     * @param opaqueUser the opaque user
     * @param apiVersion the API version
     * @param requestId the request ID
     * @return Elide response object
     */
    public ElideResponse delete(Route route, String jsonApiDocument,
                                User opaqueUser, UUID requestId) {
        UUID requestUuid = requestId != null ? requestId : UUID.randomUUID();

        return handleRequest(false, opaqueUser, dataStore::beginTransaction, requestUuid, (tx, user) -> {
            JsonApiDocument jsonApiDoc = StringUtils.isEmpty(jsonApiDocument)
                    ? new JsonApiDocument()
                    : mapper.readJsonApiDocument(jsonApiDocument);
            JsonApiRequestScope requestScope = new JsonApiRequestScope(route, tx, user, requestUuid, elideSettings,
                    jsonApiDoc);
            requestScope.setEntityProjection(new EntityProjectionMaker(elideSettings.getDictionary(),
                    requestScope).parsePath(route.getPath()));
            BaseVisitor visitor = new DeleteVisitor(requestScope);
            return visit(route.getPath(), requestScope, visitor);
        });
    }

    /**
     * Handle operations for the Atomic Operations extension.
     *
     * @param baseUrlEndPoint base URL with prefix endpoint
     * @param contentType the content type
     * @param accept the accept
     * @param path the path
     * @param jsonApiDocument the json api document
     * @param queryParams the query params
     * @param opaqueUser the opaque user
     * @param apiVersion the API version
     * @param requestId the request ID
     * @return Elide response object
     * @return
     */
    public ElideResponse operations(Route route,
            String jsonApiDocument, User opaqueUser, UUID requestId) {

        UUID requestUuid = requestId != null ? requestId : UUID.randomUUID();

        String accept = route.getHeaders().get("accept").stream().findFirst().orElse("");
        String contentType = route.getHeaders().get("content-type").stream().findFirst().orElse("");

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
            return new ElideResponse(HttpStatus.SC_UNSUPPORTED_MEDIA_TYPE, "Unsupported Media Type");
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
    protected <T> ElideResponse handleRequest(boolean isReadOnly, User user,
                                          Supplier<DataStoreTransaction> transaction, UUID requestId,
                                          Handler<DataStoreTransaction, User, HandlerResult> handler) {
        boolean isVerbose = false;
        try (DataStoreTransaction tx = transaction.get()) {
            transactionRegistry.addRunningTransaction(requestId, tx);
            HandlerResult result = handler.handle(tx, user);
            JsonApiRequestScope requestScope = result.getRequestScope();
            isVerbose = requestScope.getPermissionExecutor().isVerbose();
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

            ElideResponse response = buildResponse(responder.get());

            auditLogger.commit();
            tx.commit(requestScope);
            requestScope.runQueuedPostCommitTriggers();

            if (log.isTraceEnabled()) {
                requestScope.getPermissionExecutor().logCheckStats();
            }

            return response;
        } catch (IOException e) {
            return handleNonRuntimeException(e, isVerbose);
        } catch (RuntimeException e) {
            return handleRuntimeException(e, isVerbose);
        } finally {
            transactionRegistry.removeRunningTransaction(requestId);
            auditLogger.clear();
        }
    }

    protected ElideResponse buildErrorResponse(HttpStatusException error, boolean isVerbose) {
        if (error instanceof InternalServerErrorException) {
            log.error("Internal Server Error", error);
        }

        return buildResponse(isVerbose ? error.getVerboseErrorResponse()
                : error.getErrorResponse());
    }

    private ElideResponse handleNonRuntimeException(Exception error, boolean isVerbose) {
        CustomErrorException mappedException = getElide().mapError(error);
        if (mappedException != null) {
            return buildErrorResponse(mappedException, isVerbose);
        }

        if (error instanceof JacksonException jacksonException) {
            String message = (jacksonException.getLocation() != null
                    && jacksonException.getLocation().getSourceRef() != null)
                    ? error.getMessage() //This will leak Java class info if the location isn't known.
                    : jacksonException.getOriginalMessage();

            return buildErrorResponse(new BadRequestException(message), isVerbose);
        }

        if (error instanceof IOException) {
            log.error("IO Exception uncaught by Elide", error);
            return buildErrorResponse(new TransactionException(error), isVerbose);
        }

        log.error("Error or exception uncaught by Elide", error);
        throw new RuntimeException(error);
    }

    private ElideResponse handleRuntimeException(RuntimeException error, boolean isVerbose) {
        CustomErrorException mappedException = getElide().mapError(error);

        if (mappedException != null) {
            return buildErrorResponse(mappedException, isVerbose);
        }

        if (error instanceof WebApplicationException) {
            throw error;
        }

        if (error instanceof ForbiddenAccessException e) {
            if (log.isDebugEnabled()) {
                log.debug("{}", e.getLoggedMessage());
            }
            return buildErrorResponse(e, isVerbose);
        }

        if (error instanceof JsonPatchExtensionException e) {
            log.debug("JSON API Json Patch extension exception caught", e);
            return buildErrorResponse(e, isVerbose);
        }

        if (error instanceof JsonApiAtomicOperationsException e) {
            log.debug("JSON API Atomic Operations extension exception caught", e);
            return buildErrorResponse(e, isVerbose);
        }

        if (error instanceof HttpStatusException e) {
            log.debug("Caught HTTP status exception", e);
            return buildErrorResponse(e, isVerbose);
        }

        if (error instanceof ParseCancellationException e) {
            log.debug("Parse cancellation exception uncaught by Elide (i.e. invalid URL)", e);
            return buildErrorResponse(new InvalidURLException(e), isVerbose);
        }

        if (error instanceof ConstraintViolationException e) {
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
                    new CustomErrorException(HttpStatus.SC_BAD_REQUEST, message, errorObjectsBuilder.build()),
                    isVerbose
            );
        }

        log.error("Error or exception uncaught by Elide", error);
        throw error;
    }

    protected <T> ElideResponse buildResponse(Pair<Integer, T> response) {
        try {
            T responseNode = response.getRight();
            Integer responseCode = response.getLeft();
            String body = responseNode == null ? null : mapper.writeJsonApiDocument(responseNode);
            return new ElideResponse(responseCode, body);
        } catch (JsonProcessingException e) {
            return new ElideResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, e.toString());
        }
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
