/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide;

import com.yahoo.elide.audit.AuditLogger;
import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.HttpStatus;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.exceptions.ForbiddenAccessException;
import com.yahoo.elide.core.exceptions.HttpStatusException;
import com.yahoo.elide.core.exceptions.InvalidURLException;
import com.yahoo.elide.core.exceptions.JsonPatchExtensionException;
import com.yahoo.elide.core.exceptions.TransactionException;
import com.yahoo.elide.extensions.JsonApiPatch;
import com.yahoo.elide.extensions.PatchRequestScope;
import com.yahoo.elide.generated.parsers.CoreLexer;
import com.yahoo.elide.generated.parsers.CoreParser;
import com.yahoo.elide.jsonapi.JsonApiMapper;
import com.yahoo.elide.jsonapi.models.JsonApiDocument;
import com.yahoo.elide.parsers.DeleteVisitor;
import com.yahoo.elide.parsers.GetVisitor;
import com.yahoo.elide.parsers.PatchVisitor;
import com.yahoo.elide.parsers.PostVisitor;
import com.yahoo.elide.security.User;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.commons.lang3.tuple.Pair;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.function.Supplier;

import javax.ws.rs.core.MultivaluedMap;

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
    public ElideResponse get(
            String path,
            MultivaluedMap<String, String> queryParams,
            Object opaqueUser) {
        RequestScope requestScope = null;
        boolean isVerbose = false;
        try (DataStoreTransaction transaction = dataStore.beginReadTransaction()) {
            final User user = transaction.accessUser(opaqueUser);
            requestScope = new RequestScope(
                    path,
                    new JsonApiDocument(),
                    transaction,
                    user,
                    queryParams,
                    elideSettings);

            isVerbose = requestScope.getPermissionExecutor().isVerbose();
            GetVisitor visitor = new GetVisitor(requestScope);
            Supplier<Pair<Integer, JsonNode>> responder = visitor.visit(parse(path));
            transaction.preCommit();
            requestScope.runQueuedPreSecurityTriggers();
            requestScope.getPermissionExecutor().executeCommitChecks();
            transaction.flush(requestScope);
            ElideResponse response = buildResponse(responder.get());
            requestScope.runQueuedPreCommitTriggers();
            auditLogger.commit(requestScope);
            transaction.commit(requestScope);
            requestScope.runQueuedPostCommitTriggers();
            if (log.isTraceEnabled()) {
                requestScope.getPermissionExecutor().printCheckStats();
            }
            return response;
        } catch (ForbiddenAccessException e) {
            log.debug("{}", e.getLoggedMessage());
            return buildErrorResponse(e, isVerbose);
        } catch (HttpStatusException e) {
            return buildErrorResponse(e, isVerbose);
        } catch (IOException e) {
            return buildErrorResponse(new TransactionException(e), isVerbose);
        } catch (ParseCancellationException e) {
            return buildErrorResponse(new InvalidURLException(e), isVerbose);
        } catch (RuntimeException | Error e) {
            log.error("Exception uncaught by Elide", e);
            throw e;
        } finally {
            auditLogger.clear();
        }
    }

    /**
     * Handle POST.
     *
     * @param path the path
     * @param jsonApiDocument the json api document
     * @param opaqueUser the opaque user
     * @return Elide response object
     */
    public ElideResponse post(
            String path,
            String jsonApiDocument,
            Object opaqueUser) {
        RequestScope requestScope = null;
        boolean isVerbose = false;
        try (DataStoreTransaction transaction = dataStore.beginTransaction()) {
            User user = transaction.accessUser(opaqueUser);
            JsonApiDocument doc = mapper.readJsonApiDocument(jsonApiDocument);
            requestScope = new RequestScope(
                    path,
                    doc,
                    transaction,
                    user,
                    null,
                    elideSettings);
            isVerbose = requestScope.getPermissionExecutor().isVerbose();
            PostVisitor visitor = new PostVisitor(requestScope);
            Supplier<Pair<Integer, JsonNode>> responder = visitor.visit(parse(path));
            transaction.preCommit();
            requestScope.runQueuedPreSecurityTriggers();
            requestScope.getPermissionExecutor().executeCommitChecks();
            requestScope.saveOrCreateObjects();
            transaction.flush(requestScope);
            ElideResponse response = buildResponse(responder.get());
            requestScope.runQueuedPreCommitTriggers();
            auditLogger.commit(requestScope);
            transaction.commit(requestScope);
            requestScope.runQueuedPostCommitTriggers();
            if (log.isTraceEnabled()) {
                requestScope.getPermissionExecutor().printCheckStats();
            }
            return response;
        } catch (ForbiddenAccessException e) {
            log.debug("{}", e.getLoggedMessage());
            return buildErrorResponse(e, isVerbose);
        } catch (HttpStatusException e) {
            return buildErrorResponse(e, isVerbose);
        } catch (IOException e) {
            return buildErrorResponse(new TransactionException(e), isVerbose);
        } catch (ParseCancellationException e) {
            return buildErrorResponse(new InvalidURLException(e), isVerbose);
        } catch (RuntimeException | Error e) {
            log.error("Exception uncaught by Elide", e);
            throw e;
        } finally {
            auditLogger.clear();
        }
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
    public ElideResponse patch(
            String contentType,
            String accept,
            String path,
            String jsonApiDocument,
            Object opaqueUser) {
        RequestScope requestScope = null;
        boolean isVerbose = false;
        try (DataStoreTransaction transaction = dataStore.beginTransaction()) {
            User user = transaction.accessUser(opaqueUser);

            Supplier<Pair<Integer, JsonNode>> responder;
            if (JsonApiPatch.isPatchExtension(contentType) && JsonApiPatch.isPatchExtension(accept)) {
                // build Outer RequestScope to be used for each action
                PatchRequestScope patchRequestScope = new PatchRequestScope(
                        path,
                        transaction,
                        user,
                        elideSettings);
                requestScope = patchRequestScope;
                isVerbose = requestScope.getPermissionExecutor().isVerbose();
                responder = JsonApiPatch.processJsonPatch(dataStore, path, jsonApiDocument, patchRequestScope);
            } else {
                JsonApiDocument doc = mapper.readJsonApiDocument(jsonApiDocument);
                requestScope = new RequestScope(
                        path,
                        doc,
                        transaction,
                        user,
                        (MultivaluedMap) null,
                        elideSettings);
                isVerbose = requestScope.getPermissionExecutor().isVerbose();
                PatchVisitor visitor = new PatchVisitor(requestScope);
                responder = visitor.visit(parse(path));
            }
            transaction.preCommit();
            requestScope.runQueuedPreSecurityTriggers();
            requestScope.getPermissionExecutor().executeCommitChecks();
            requestScope.saveOrCreateObjects();
            transaction.flush(requestScope);
            ElideResponse response = buildResponse(responder.get());
            requestScope.runQueuedPreCommitTriggers();
            auditLogger.commit(requestScope);
            transaction.commit(requestScope);
            requestScope.runQueuedPostCommitTriggers();
            if (log.isTraceEnabled()) {
                requestScope.getPermissionExecutor().printCheckStats();
            }
            return response;
        } catch (ForbiddenAccessException e) {
            log.debug("{}", e.getLoggedMessage());
            return buildErrorResponse(e, isVerbose);
        } catch (JsonPatchExtensionException e) {
            return buildResponse(e.getResponse());
        } catch (HttpStatusException e) {
            return buildErrorResponse(e, isVerbose);
        } catch (ParseCancellationException e) {
            return buildErrorResponse(new InvalidURLException(e), isVerbose);
        } catch (IOException e) {
            return buildErrorResponse(new TransactionException(e), isVerbose);
        } catch (RuntimeException | Error e) {
            log.error("Exception uncaught by Elide", e);
            throw e;
        } finally {
            auditLogger.clear();
        }
    }

    /**
     * Handle DELETE.
     *
     * @param path the path
     * @param jsonApiDocument the json api document
     * @param opaqueUser the opaque user
     * @return Elide response object
     */
    public ElideResponse delete(
            String path,
            String jsonApiDocument,
            Object opaqueUser) {
        JsonApiDocument doc;
        RequestScope requestScope = null;
        boolean isVerbose = false;
        try (DataStoreTransaction transaction = dataStore.beginTransaction()) {
            User user = transaction.accessUser(opaqueUser);
            if (jsonApiDocument != null && !jsonApiDocument.equals("")) {
                doc = mapper.readJsonApiDocument(jsonApiDocument);
            } else {
                doc = new JsonApiDocument();
            }
            requestScope = new RequestScope(
                    path,
                    doc,
                    transaction,
                    user,
                    (MultivaluedMap) null,
                    elideSettings);
            isVerbose = requestScope.getPermissionExecutor().isVerbose();
            DeleteVisitor visitor = new DeleteVisitor(requestScope);
            Supplier<Pair<Integer, JsonNode>> responder = visitor.visit(parse(path));
            transaction.preCommit();
            requestScope.runQueuedPreSecurityTriggers();
            requestScope.getPermissionExecutor().executeCommitChecks();
            requestScope.saveOrCreateObjects();
            transaction.flush(requestScope);
            ElideResponse response = buildResponse(responder.get());
            requestScope.runQueuedPreCommitTriggers();
            auditLogger.commit(requestScope);
            transaction.commit(requestScope);
            requestScope.runQueuedPostCommitTriggers();
            if (log.isTraceEnabled()) {
                requestScope.getPermissionExecutor().printCheckStats();
            }
            return response;
        } catch (ForbiddenAccessException e) {
            log.debug("{}", e.getLoggedMessage());
            return buildErrorResponse(e, isVerbose);
        } catch (HttpStatusException e) {
            return buildErrorResponse(e, isVerbose);
        } catch (IOException e) {
            return buildErrorResponse(new TransactionException(e), isVerbose);
        } catch (ParseCancellationException e) {
            return buildErrorResponse(new InvalidURLException(e), isVerbose);
        } catch (RuntimeException | Error e) {
            log.error("Exception uncaught by Elide", e);
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
        path = Paths.get(path).normalize().toString().replace(File.separatorChar, '/');
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        ANTLRInputStream is = new ANTLRInputStream(path);
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
}
