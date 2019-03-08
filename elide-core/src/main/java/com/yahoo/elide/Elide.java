/*
 * Copyright 2018, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide;

import com.yahoo.elide.audit.AuditLogger;
import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.ErrorObjects;
import com.yahoo.elide.core.HttpStatus;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.datastore.inmemory.InMemoryDataStore;
import com.yahoo.elide.core.exceptions.CustomErrorException;
import com.yahoo.elide.core.exceptions.ForbiddenAccessException;
import com.yahoo.elide.core.exceptions.HttpStatusException;
import com.yahoo.elide.core.exceptions.InternalServerErrorException;
import com.yahoo.elide.core.exceptions.InvalidConstraintException;
import com.yahoo.elide.core.exceptions.InvalidURLException;
import com.yahoo.elide.core.exceptions.JsonPatchExtensionException;
import com.yahoo.elide.core.exceptions.TransactionException;
import com.yahoo.elide.extensions.JsonApiPatch;
import com.yahoo.elide.extensions.PatchRequestScope;
import com.yahoo.elide.jsonapi.JsonApiMapper;
import com.yahoo.elide.jsonapi.models.JsonApiDocument;
import com.yahoo.elide.parsers.BaseVisitor;
import com.yahoo.elide.parsers.DeleteVisitor;
import com.yahoo.elide.parsers.GetVisitor;
import com.yahoo.elide.parsers.JsonApiParser;
import com.yahoo.elide.parsers.PatchVisitor;
import com.yahoo.elide.parsers.PostVisitor;
import com.yahoo.elide.security.User;
import com.yahoo.elide.utils.coerce.CoerceUtil;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.function.Supplier;

import javax.validation.ConstraintViolationException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MultivaluedMap;

/**
 * REST Entry point handler.
 */
@Slf4j
public class Elide {

    @Getter private final ElideSettings elideSettings;
    @Getter private final AuditLogger auditLogger;
    @Getter private final DataStore dataStore;
    @Getter private final JsonApiMapper mapper;

    /**
     * Instantiates a new Elide instance.
     *
     * @param elideSettings Elide settings object
     */
    public Elide(ElideSettings elideSettings) {
        this.elideSettings = elideSettings;
        this.auditLogger = elideSettings.getAuditLogger();
        this.dataStore = new InMemoryDataStore(elideSettings.getDataStore());
        this.dataStore.populateEntityDictionary(elideSettings.getDictionary());
        this.mapper = elideSettings.getMapper();

        elideSettings.getSerdes().forEach((targetType, serde) -> {
            CoerceUtil.register(targetType, serde);
        });
    }

    /**
     * Handle GET.
     *
     * @param path the path
     * @param queryParams the query params
     * @param opaqueUser the opaque user
     * @return Elide response object
     */
    public ElideResponse get(String path, MultivaluedMap<String, String> queryParams, Object opaqueUser) {
        return handleRequest(true, opaqueUser, dataStore::beginReadTransaction, (tx, user) -> {
            JsonApiDocument jsonApiDoc = new JsonApiDocument();
            RequestScope requestScope = new RequestScope(path, jsonApiDoc, tx, user, queryParams, elideSettings, false);
            BaseVisitor visitor = new GetVisitor(requestScope);
            return visit(path, requestScope, visitor);
        });

    }

    /**
     * Handle POST.
     *
     * @param path the path
     * @param jsonApiDocument the json api document
     * @param opaqueUser the opaque user
     * @return Elide response object
     */
    public ElideResponse post(String path, String jsonApiDocument, Object opaqueUser) {
        return handleRequest(false, opaqueUser, dataStore::beginTransaction, (tx, user) -> {
            JsonApiDocument jsonApiDoc = mapper.readJsonApiDocument(jsonApiDocument);
            RequestScope requestScope = new RequestScope(path, jsonApiDoc, tx, user, null, elideSettings, false);
            BaseVisitor visitor = new PostVisitor(requestScope);
            return visit(path, requestScope, visitor);
        });
    }

    /**
     * Handle PATCH.
     *
     * @param contentType the content type
     * @param accept the accept
     * @param path the path
     * @param jsonApiDocument the json api document
     * @param opaqueUser the opaque user
     * @return Elide response object
     */
    public ElideResponse patch(String contentType, String accept,
                               String path, String jsonApiDocument, Object opaqueUser) {

        Handler<DataStoreTransaction, User, HandlerResult> handler;
        if (JsonApiPatch.isPatchExtension(contentType) && JsonApiPatch.isPatchExtension(accept)) {
            handler = (tx, user) -> {
                PatchRequestScope requestScope = new PatchRequestScope(path, tx, user, elideSettings);
                try {
                    Supplier<Pair<Integer, JsonNode>> responder =
                            JsonApiPatch.processJsonPatch(dataStore, path, jsonApiDocument, requestScope);
                    return new HandlerResult(requestScope, responder);
                } catch (RuntimeException e) {
                    return new HandlerResult(requestScope, e);
                }
            };
        } else {
            handler = (tx, user) -> {
                JsonApiDocument jsonApiDoc = mapper.readJsonApiDocument(jsonApiDocument);
                RequestScope requestScope = new RequestScope(path, jsonApiDoc, tx, user, null, elideSettings, false);
                BaseVisitor visitor = new PatchVisitor(requestScope);
                return visit(path, requestScope, visitor);
            };
        }

        return handleRequest(false, opaqueUser, dataStore::beginTransaction, handler);
    }

    /**
     * Handle DELETE.
     *
     * @param path the path
     * @param jsonApiDocument the json api document
     * @param opaqueUser the opaque user
     * @return Elide response object
     */
    public ElideResponse delete(String path, String jsonApiDocument, Object opaqueUser) {
        return handleRequest(false, opaqueUser, dataStore::beginTransaction, (tx, user) -> {
            JsonApiDocument jsonApiDoc = StringUtils.isEmpty(jsonApiDocument)
                    ? new JsonApiDocument()
                    : mapper.readJsonApiDocument(jsonApiDocument);
            RequestScope requestScope = new RequestScope(path, jsonApiDoc, tx, user, null, elideSettings, false);
            BaseVisitor visitor = new DeleteVisitor(requestScope);
            return visit(path, requestScope, visitor);
        });
    }

    public HandlerResult visit(String path, RequestScope requestScope, BaseVisitor visitor) {
        try {
            Supplier<Pair<Integer, JsonNode>> responder = visitor.visit(JsonApiParser.parse(path));
            return new HandlerResult(requestScope, responder);
        } catch (RuntimeException e) {
            return new HandlerResult(requestScope, e);
        }
    }

    /**
     * Handle JSON API requests.
     *
     * @param isReadOnly if the transaction is read only
     * @param opaqueUser the user object from the container
     * @param transaction a transaction supplier
     * @param handler a function that creates the request scope and request handler
     * @return the response
     */
    protected ElideResponse handleRequest(boolean isReadOnly, Object opaqueUser,
                                          Supplier<DataStoreTransaction> transaction,
                                          Handler<DataStoreTransaction, User, HandlerResult> handler) {
        boolean isVerbose = false;
        try (DataStoreTransaction tx = transaction.get()) {
            final User user = tx.accessUser(opaqueUser);
            HandlerResult result = handler.handle(tx, user);
            RequestScope requestScope = result.getRequestScope();
            isVerbose = requestScope.getPermissionExecutor().isVerbose();
            Supplier<Pair<Integer, JsonNode>> responder = result.getResponder();
            tx.preCommit();
            requestScope.runQueuedPreSecurityTriggers();
            requestScope.getPermissionExecutor().executeCommitChecks();
            if (!isReadOnly) {
                requestScope.saveOrCreateObjects();
            }
            tx.flush(requestScope);

            requestScope.runQueuedPreCommitTriggers();

            ElideResponse response = buildResponse(responder.get());

            auditLogger.commit(requestScope);
            tx.commit(requestScope);
            requestScope.runQueuedPostCommitTriggers();

            if (log.isTraceEnabled()) {
                requestScope.getPermissionExecutor().printCheckStats();
            }

            return response;

        } catch (WebApplicationException e) {
            throw e;

        } catch (ForbiddenAccessException e) {
            if (log.isDebugEnabled()) {
                log.debug("{}", e.getLoggedMessage());
            }
            return buildErrorResponse(e, isVerbose);

        } catch (JsonPatchExtensionException e) {
            log.debug("JSON patch extension exception caught", e);
            return buildErrorResponse(e, isVerbose);

        } catch (HttpStatusException e) {
            log.debug("Caught HTTP status exception", e);
            return buildErrorResponse(e, isVerbose);

        } catch (IOException e) {
            log.error("IO Exception uncaught by Elide", e);
            return buildErrorResponse(new TransactionException(e), isVerbose);

        } catch (ParseCancellationException e) {
            log.debug("Parse cancellation exception uncaught by Elide (i.e. invalid URL)", e);
            return buildErrorResponse(new InvalidURLException(e), isVerbose);

        } catch (ConstraintViolationException e) {
            log.debug("Constraint violation exception caught", e);
            String message = "Constraint violation";
            if (!e.getConstraintViolations().isEmpty()) {
                // Return error for the first constraint violation
                message = e.getConstraintViolations().iterator().next().getMessage();
            }
            return buildErrorResponse(new InvalidConstraintException(message), isVerbose);

        } catch (Exception | Error e) {
            log.error("Error or exception uncaught by Elide", e);
            throw e;

        } finally {
            auditLogger.clear();
        }
    }

    protected ElideResponse buildErrorResponse(HttpStatusException error, boolean isVerbose) {
        if (error instanceof InternalServerErrorException) {
            log.error("Internal Server Error", error);
        }
        if (!(error instanceof CustomErrorException) && elideSettings.isReturnErrorObjects()) {
            ErrorObjects errors = ErrorObjects.builder().addError()
                    .withDetail(isVerbose ? error.getVerboseMessage() : error.toString()).build();
            JsonNode responseBody = mapper.getObjectMapper().convertValue(errors, JsonNode.class);
            return buildResponse(Pair.of(error.getStatus(), responseBody));
        }
        return buildResponse(isVerbose ? error.getVerboseErrorResponse() : error.getErrorResponse());
    }

    protected ElideResponse buildResponse(Pair<Integer, JsonNode> response) {
        try {
            JsonNode responseNode = response.getRight();
            Integer responseCode = response.getLeft();
            String body = responseNode == null ? null : mapper.writeJsonApiDocument(responseNode);
            return new ElideResponse(responseCode, body);
        } catch (JsonProcessingException e) {
            return new ElideResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, e.toString());
        }
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
     */
    protected static class HandlerResult {
        protected RequestScope requestScope;
        protected Supplier<Pair<Integer, JsonNode>> result;
        protected RuntimeException cause;

        protected HandlerResult(RequestScope requestScope, Supplier<Pair<Integer, JsonNode>> result) {
            this.requestScope = requestScope;
            this.result = result;
        }

        public HandlerResult(RequestScope requestScope, RuntimeException cause) {
            this.requestScope = requestScope;
            this.cause = cause;
        }

        public Supplier<Pair<Integer, JsonNode>> getResponder() {
            if (cause != null) {
                throw cause;
            }
            return result;
        }

        public RequestScope getRequestScope() {
            return requestScope;
        }
    }
}
