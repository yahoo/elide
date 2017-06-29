/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.yahoo.elide.audit.AuditLogger;
import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.HttpStatus;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.exceptions.ForbiddenAccessException;
import com.yahoo.elide.core.exceptions.HttpStatusException;
import com.yahoo.elide.core.exceptions.InternalServerErrorException;
import com.yahoo.elide.core.exceptions.InvalidURLException;
import com.yahoo.elide.core.exceptions.JsonPatchExtensionException;
import com.yahoo.elide.core.exceptions.TransactionException;
import com.yahoo.elide.extensions.JsonApiPatch;
import com.yahoo.elide.extensions.PatchRequestScope;
import com.yahoo.elide.generated.parsers.CoreLexer;
import com.yahoo.elide.generated.parsers.CoreParser;
import com.yahoo.elide.jsonapi.JsonApiMapper;
import com.yahoo.elide.jsonapi.models.JsonApiDocument;
import com.yahoo.elide.parsers.BaseVisitor;
import com.yahoo.elide.parsers.DeleteVisitor;
import com.yahoo.elide.parsers.GetVisitor;
import com.yahoo.elide.parsers.PatchVisitor;
import com.yahoo.elide.parsers.PostVisitor;
import com.yahoo.elide.security.User;
import lombok.extern.slf4j.Slf4j;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MultivaluedMap;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.function.Supplier;

/**
 * REST Entry point handler.
 */
@Slf4j
public class Elide {
    private final ElideSettings elideSettings;
    private final AuditLogger auditLogger;
    private final DataStore dataStore;
    private final JsonApiMapper mapper;

    /**
     * Instantiates a new Elide instance.
     *
     * @param elideSettings Elide settings object
     */
    public Elide(ElideSettings elideSettings) {
        this.elideSettings = elideSettings;
        this.auditLogger = elideSettings.getAuditLogger();
        this.dataStore = elideSettings.getDataStore();
        this.dataStore.populateEntityDictionary(elideSettings.getDictionary());
        this.mapper = elideSettings.getMapper();
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
            RequestScope requestScope = new RequestScope(path, jsonApiDoc, tx, user, queryParams, elideSettings);
            BaseVisitor visitor = new GetVisitor(requestScope);
            Supplier<Pair<Integer, JsonNode>> responder = visitor.visit(parse(path));

            return new HandlerResult(requestScope, responder);
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
            RequestScope requestScope = new RequestScope(path, jsonApiDoc, tx, user, null, elideSettings);
            BaseVisitor visitor = new PostVisitor(requestScope);
            Supplier<Pair<Integer, JsonNode>> responder = visitor.visit(parse(path));

            return new HandlerResult(requestScope, responder);
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
                Supplier<Pair<Integer, JsonNode>> responder =
                        JsonApiPatch.processJsonPatch(dataStore, path, jsonApiDocument, requestScope);
                return new HandlerResult(requestScope, responder);
            };
        } else {
            handler = (tx, user) -> {
                JsonApiDocument jsonApiDoc = mapper.readJsonApiDocument(jsonApiDocument);
                RequestScope requestScope = new RequestScope(path, jsonApiDoc, tx, user, null, elideSettings);
                BaseVisitor visitor = new PatchVisitor(requestScope);
                Supplier<Pair<Integer, JsonNode>> responder = visitor.visit(parse(path));

                return new HandlerResult(requestScope, responder);
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
            RequestScope requestScope = new RequestScope(path, jsonApiDoc, tx, user, null, elideSettings);
            BaseVisitor visitor = new DeleteVisitor(requestScope);
            try {
                Supplier<Pair<Integer, JsonNode>> responder = visitor.visit(parse(path));
                return new HandlerResult(requestScope, responder);
            } catch (RuntimeException e) {
                return new HandlerResult(requestScope, e);
            }
        });
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

            ElideResponse response = buildResponse(responder.get());

            requestScope.runQueuedPreCommitTriggers();
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
            log.debug("{}", e.getLoggedMessage());
            return buildErrorResponse(e, isVerbose);

        } catch (JsonPatchExtensionException e) {
            return buildResponse(e.getResponse());

        } catch (HttpStatusException e) {
            return buildErrorResponse(e, isVerbose);

        } catch (IOException e) {
            return buildErrorResponse(new TransactionException(e), isVerbose);

        } catch (ParseCancellationException e) {
            return buildErrorResponse(new InvalidURLException(e), isVerbose);

        } catch (Exception e) {
            return buildErrorResponse(new InternalServerErrorException(e), isVerbose);

        } catch (Error e) {
            log.error("Error uncaught by Elide", e);
            throw e;

        } finally {
            auditLogger.clear();
        }
    }

    /**
     * Compile request to AST.
     *
     * @param path request
     * @return AST parse tree
     */
    public static ParseTree parse(String path) {
        String normalizedPath = Paths.get(path).normalize().toString().replace(File.separatorChar, '/');
        if (normalizedPath.startsWith("/")) {
            normalizedPath = normalizedPath.substring(1);
        }
        ANTLRInputStream is = new ANTLRInputStream(normalizedPath);
        CoreLexer lexer = new CoreLexer(is);
        lexer.removeErrorListeners();
        lexer.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line,
                                    int charPositionInLine, String msg, RecognitionException e) {
                throw new ParseCancellationException(msg, e);
            }
        });
        CoreParser parser = new CoreParser(new CommonTokenStream(lexer));
        parser.setErrorHandler(new BailErrorStrategy());
        return parser.start();
    }

    protected ElideResponse buildErrorResponse(HttpStatusException error, boolean isVerbose) {
        if (error instanceof InternalServerErrorException) {
            log.error("Internal Server Error", error);
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
     * Get ElideSettings.
     * @return settings
     */
    public ElideSettings getElideSettings() {
        return elideSettings;
    }

    /**
     * Get AuditLogger.
     * @return AuditLogger
     */
    public AuditLogger getAuditLogger() {
        return auditLogger;
    }

    /**
     * Get DataStore.
     * @return DataStore
     */
    public DataStore getDataStore() {
        return dataStore;
    }

    /**
     * Get JsonApiMapper.
     * @return JsonApiMapper
     */
    public JsonApiMapper getMapper() {
        return mapper;
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
