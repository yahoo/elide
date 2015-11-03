/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide;

import com.yahoo.elide.audit.Logger;
import com.yahoo.elide.core.DatabaseManager;
import com.yahoo.elide.core.DatabaseTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.HttpStatus;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.SecurityMode;
import com.yahoo.elide.core.exceptions.HttpStatusException;
import com.yahoo.elide.core.exceptions.InvalidURLException;
import com.yahoo.elide.core.exceptions.TransactionException;
import com.yahoo.elide.extensions.JsonApiPatch;
import com.yahoo.elide.extensions.PatchRequestScope;
import com.yahoo.elide.jsonapi.JsonApiMapper;
import com.yahoo.elide.jsonapi.models.JsonApiDocument;
import com.yahoo.elide.parsers.DeleteVisitor;
import com.yahoo.elide.parsers.GetVisitor;
import com.yahoo.elide.parsers.PatchVisitor;
import com.yahoo.elide.parsers.PostVisitor;
import com.yahoo.elide.parsers.ormLexer;
import com.yahoo.elide.parsers.ormParser;
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

import java.io.IOException;
import java.nio.file.Paths;
import java.util.function.Supplier;

import javax.ws.rs.core.MultivaluedMap;

/**
 * REST Entry point handler.
 */
@SuppressWarnings("static-method")
public class Elide {

    private final Logger auditLogger;
    private final DatabaseManager db;
    private final EntityDictionary dictionary;
    private final JsonApiMapper mapper;
    /**
     * Instantiates a new Elide.
     *
     * @param auditLogger the audit logger
     * @param db the db
     * @param dictionary the dictionary
     */
    public Elide(Logger auditLogger, DatabaseManager db, EntityDictionary dictionary) {
        this.auditLogger = auditLogger;
        this.db = db;
        this.dictionary = dictionary;
        db.populateEntityDictionary(dictionary);
        this.mapper = new JsonApiMapper(dictionary);
    }

    /**
     * Instantiates a new Elide.
     *
     * @param auditLogger the audit logger
     * @param db the db
     */
    public Elide(Logger auditLogger, DatabaseManager db) {
        this(auditLogger, db, new EntityDictionary());
    }

    /**
     * Handle GET.
     *
     * @param path the path
     * @param queryParams the query params
     * @param opaqueUser the opaque user
     * @param securityMode only for test mode
     * @return Elide response object
     */
    public ElideResponse get(
            String path,
            MultivaluedMap<String, String> queryParams,
            Object opaqueUser,
            SecurityMode securityMode) {

        try (DatabaseTransaction transaction = db.beginReadTransaction()) {
            User user = transaction.accessUser(opaqueUser);
            RequestScope requestScope = new RequestScope(
                    new JsonApiDocument(),
                    transaction,
                    user,
                    dictionary,
                    mapper,
                    auditLogger,
                    queryParams,
                    securityMode);
            GetVisitor visitor = new GetVisitor(requestScope);
            Supplier<Pair<Integer, JsonNode>> responder = visitor.visit(parse(path));
            requestScope.runDeferredPermissionChecks();
            transaction.flush();
            ElideResponse response = buildResponse(responder.get());
            auditLogger.commit();
            transaction.commit();
            return response;
        } catch (HttpStatusException e) {
            return buildErrorResponse(e);
        } catch (IOException e) {
            return buildErrorResponse(new TransactionException(e));
        } catch (ParseCancellationException e) {
            return buildErrorResponse(new InvalidURLException(e));
        }
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
        return this.get(path, queryParams, opaqueUser, SecurityMode.ACTIVE);
    }

    /**
     * Handle POST.
     *
     * @param path the path
     * @param jsonApiDocument the json api document
     * @param opaqueUser the opaque user
     * @param securityMode only for test mode
     * @return Elide response object
     */
    public ElideResponse post(
            String path,
            String jsonApiDocument,
            Object opaqueUser,
            SecurityMode securityMode) {
        try (DatabaseTransaction transaction = db.beginTransaction()) {
            User user = transaction.accessUser(opaqueUser);
            JsonApiDocument doc = mapper.readJsonApiDocument(jsonApiDocument);
            RequestScope requestScope = new RequestScope(doc,
                    transaction,
                    user,
                    dictionary,
                    mapper,
                    auditLogger,
                    securityMode);
            PostVisitor visitor = new PostVisitor(requestScope);
            Supplier<Pair<Integer, JsonNode>> responder = visitor.visit(parse(path));
            requestScope.runDeferredPermissionChecks();
            transaction.flush();
            ElideResponse response = buildResponse(responder.get());
            auditLogger.commit();
            transaction.commit();
            return response;
        } catch (HttpStatusException e) {
            return buildErrorResponse(e);
        } catch (IOException e) {
            return buildErrorResponse(new TransactionException(e));
        } catch (ParseCancellationException e) {
            return buildErrorResponse(new InvalidURLException(e));
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
        return this.post(path, jsonApiDocument, opaqueUser, SecurityMode.ACTIVE);
    }

    /**
     * Handle PATCH.
     *
     * @param contentType the content type
     * @param accept the accept
     * @param path the path
     * @param jsonApiDocument the json api document
     * @param opaqueUser the opaque user
     * @param securityMode only for test mode
     * @return Elide response object
     */
    public ElideResponse patch(
            String contentType,
            String accept,
            String path,
            String jsonApiDocument,
            Object opaqueUser,
            SecurityMode securityMode) {
        try (DatabaseTransaction transaction = db.beginTransaction()) {
            User user = transaction.accessUser(opaqueUser);

            RequestScope requestScope;
            Supplier<Pair<Integer, JsonNode>> responder;
            if (JsonApiPatch.isPatchExtension(contentType) && JsonApiPatch.isPatchExtension(accept)) {
                // build Outer RequestScope to be used for each action
                PatchRequestScope patchRequestScope = new PatchRequestScope(
                        transaction, user, dictionary, mapper, auditLogger);
                requestScope = patchRequestScope;
                responder = JsonApiPatch.processJsonPatch(db, path, jsonApiDocument, patchRequestScope);
            } else {
                JsonApiDocument doc = mapper.readJsonApiDocument(jsonApiDocument);
                requestScope = new RequestScope(doc, transaction, user, dictionary, mapper, auditLogger, securityMode);
                PatchVisitor visitor = new PatchVisitor(requestScope);
                responder = visitor.visit(parse(path));
            }
            requestScope.runDeferredPermissionChecks();
            transaction.flush();
            ElideResponse response = buildResponse(responder.get());
            auditLogger.commit();
            transaction.commit();
            return response;
        } catch (HttpStatusException e) {
            return buildErrorResponse(e);
        } catch (ParseCancellationException e) {
            return buildErrorResponse(new InvalidURLException(e));
        } catch (IOException e) {
            return buildErrorResponse(new TransactionException(e));
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
        return this.patch(contentType, accept, path, jsonApiDocument, opaqueUser, SecurityMode.ACTIVE);
    }

    /**
     * Handle DELETE.
     *
     * @param path the path
     * @param jsonApiDocument the json api document
     * @param opaqueUser the opaque user
     * @param securityMode only for test mode
     * @return Elide response object
     */
    public ElideResponse delete(
            String path,
            String jsonApiDocument,
            Object opaqueUser,
            SecurityMode securityMode) {
        JsonApiDocument doc;
        try (DatabaseTransaction transaction = db.beginTransaction()) {
            User user = transaction.accessUser(opaqueUser);
            if (jsonApiDocument != null && !jsonApiDocument.equals("")) {
                doc = mapper.readJsonApiDocument(jsonApiDocument);
            } else {
                doc = new JsonApiDocument();
            }
            RequestScope requestScope = new RequestScope(
                    doc, transaction, user, dictionary, mapper, auditLogger, securityMode);
            DeleteVisitor visitor = new DeleteVisitor(requestScope);
            Supplier<Pair<Integer, JsonNode>> responder = visitor.visit(parse(path));
            requestScope.runDeferredPermissionChecks();
            transaction.flush();
            ElideResponse response = buildResponse(responder.get());
            auditLogger.commit();
            transaction.commit();
            return response;
        } catch (HttpStatusException e) {
            return buildErrorResponse(e);
        } catch (IOException e) {
            return buildErrorResponse(new TransactionException(e));
        } catch (ParseCancellationException e) {
            return buildErrorResponse(new InvalidURLException(e));
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
        return this.delete(path, jsonApiDocument, opaqueUser, SecurityMode.ACTIVE);
    }

    /**
     * Compile request to AST.
     *
     * @param path request
     * @return AST parse tree
     */
    public static ParseTree parse(String path) {
        path = Paths.get(path).normalize().toString();
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        ANTLRInputStream is = new ANTLRInputStream(path);
        ormLexer lexer = new ormLexer(is);
        lexer.removeErrorListeners();
        lexer.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line,
                    int charPositionInLine, String msg, RecognitionException e) {
                throw new ParseCancellationException(msg, e);
            }
        });
        ormParser parser = new ormParser(new CommonTokenStream(lexer));
        parser.setErrorHandler(new BailErrorStrategy());
        return parser.start();
    }

    protected ElideResponse buildErrorResponse(HttpStatusException error) {
        return buildResponse(error.getErrorResponse());
    }

    protected ElideResponse buildResponse(Pair<Integer, JsonNode> response) {
        try {
            JsonNode responseNode = response.getRight();
            Integer responseCode = response.getLeft();
            String body = mapper.writeJsonApiDocument(responseNode);
            return new ElideResponse(responseCode, body);
        } catch (JsonProcessingException e) {
            return new ElideResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, e.toString());
        }
    }
}
