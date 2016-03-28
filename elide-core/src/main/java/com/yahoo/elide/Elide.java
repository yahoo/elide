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
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.HttpStatus;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.exceptions.ForbiddenAccessException;
import com.yahoo.elide.core.exceptions.JsonPatchExtensionException;
import com.yahoo.elide.security.PermissionExecutor;
import com.yahoo.elide.security.SecurityMode;
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
import com.yahoo.elide.generated.parsers.CoreLexer;
import com.yahoo.elide.generated.parsers.CoreParser;
import com.yahoo.elide.security.User;
import com.yahoo.elide.security.executors.ActivePermissionExecutor;
import lombok.extern.slf4j.Slf4j;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.commons.lang3.tuple.Pair;

import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Paths;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * REST Entry point handler.
 */
@Slf4j
public class Elide {

    private final AuditLogger auditLogger;
    private final DataStore dataStore;
    private final EntityDictionary dictionary;
    private final JsonApiMapper mapper;
    private final Function<RequestScope, PermissionExecutor> permissionExecutor;

    /**
     * Instantiates a new Elide.
     *
     * @param auditLogger the audit logger
     * @param dataStore the dataStore
     * @param dictionary the dictionary
     */
    @Deprecated
    public Elide(AuditLogger auditLogger, DataStore dataStore, EntityDictionary dictionary) {
        this(auditLogger, dataStore, dictionary, new JsonApiMapper(dictionary));
    }

    /**
     * Instantiates a new Elide.
     *
     * @param auditLogger the audit logger
     * @param dataStore the dataStore
     */
    @Deprecated
    public Elide(AuditLogger auditLogger, DataStore dataStore) {
        this(auditLogger, dataStore, new EntityDictionary());
    }

    /**
     * Instantiates a new Elide.
     *
     * @param auditLogger the audit logger
     * @param dataStore the dataStore
     * @param dictionary the dictionary
     * @param mapper Serializer/Deserializer for JSON API
     */
    @Deprecated
    public Elide(AuditLogger auditLogger, DataStore dataStore, EntityDictionary dictionary, JsonApiMapper mapper) {
        this(auditLogger, dataStore, dictionary, mapper, ActivePermissionExecutor::new);
    }

    /**
     * Instantiates a new Elide.
     *
     * @param auditLogger the audit logger
     * @param dataStore the dataStore
     * @param dictionary the dictionary
     * @param mapper Serializer/Deserializer for JSON API
     * @param permissionExecutor Custom permission executor implementation
     */
    private Elide(AuditLogger auditLogger,
                  DataStore dataStore,
                  EntityDictionary dictionary,
                  JsonApiMapper mapper,
                  Function<RequestScope, PermissionExecutor> permissionExecutor) {
        this.auditLogger = auditLogger;
        this.dataStore = dataStore;
        this.dictionary = dictionary;
        dataStore.populateEntityDictionary(dictionary);
        this.mapper = mapper;
        this.permissionExecutor = permissionExecutor;
    }

    /**
     * Elide Builder for constructing an Elide instance.
     */
    public static class Builder {
        private final AuditLogger auditLogger;
        private final DataStore dataStore;
        private EntityDictionary entityDictionary;
        private JsonApiMapper jsonApiMapper;
        private Function<RequestScope, PermissionExecutor> permissionExecutorFunction;

        /**
         * Constructor.
         *
         * @param auditLogger
         * @param dataStore
         */
        public Builder(AuditLogger auditLogger, DataStore dataStore) {
            this.auditLogger = auditLogger;
            this.dataStore = dataStore;
            this.entityDictionary = new EntityDictionary();
            this.jsonApiMapper = new JsonApiMapper(entityDictionary);
            this.permissionExecutorFunction = null;
        }

        public Elide build() {
            if (permissionExecutorFunction == null) {
                // If nothing is set, provide default
                permissionExecutorFunction = ActivePermissionExecutor::new;
            }
            return new Elide(auditLogger, dataStore, entityDictionary, jsonApiMapper, permissionExecutorFunction);
        }

        public Builder entityDictionary(final EntityDictionary entityDictionary) {
            this.entityDictionary = entityDictionary;
            return this;
        }

        public Builder jsonApiMapper(final JsonApiMapper jsonApiMapper) {
            this.jsonApiMapper = jsonApiMapper;
            return this;
        }

        public Builder permissionExecutor(final Function<RequestScope, PermissionExecutor> permissionExecutorFunction) {
            this.permissionExecutorFunction = permissionExecutorFunction;
            return this;
        }

        public Builder permissionExecutor(final Class<? extends PermissionExecutor> permissionExecutorClass) {
            permissionExecutorFunction = (requestScope) -> {
                try {
                    try {
                        // Try to find a constructor with request scope
                        Constructor<? extends PermissionExecutor> ctor =
                                permissionExecutorClass.getDeclaredConstructor(RequestScope.class);
                        return ctor.newInstance(requestScope);
                    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException
                            | InstantiationException e) {
                        // If that fails, try blank constructor
                        return permissionExecutorClass.newInstance();
                    }
                } catch (IllegalAccessException | InstantiationException e) {
                    // Everything failed. Throw hands up, not sure how to proceed.
                    throw new RuntimeException(e);
                }
            };
            return this;
        }
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

        RequestScope requestScope = null;
        try (DataStoreTransaction transaction = dataStore.beginReadTransaction()) {
            final User user = transaction.accessUser(opaqueUser);
            requestScope = new RequestScope(
                    new JsonApiDocument(),
                    transaction,
                    user,
                    dictionary,
                    mapper,
                    auditLogger,
                    queryParams,
                    securityMode,
                    permissionExecutor);
            GetVisitor visitor = new GetVisitor(requestScope);
            Supplier<Pair<Integer, JsonNode>> responder = visitor.visit(parse(path));
            requestScope.getPermissionExecutor().executeCommitChecks();
            transaction.flush();
            ElideResponse response = buildResponse(responder.get());
            auditLogger.commit();
            transaction.commit();
            requestScope.runCommitTriggers();
            traceLogSecurityExceptions(requestScope);
            return response;
        } catch (ForbiddenAccessException e) {
            debugLogSecurityExceptions(requestScope);
            return buildErrorResponse(e, securityMode);
        } catch (HttpStatusException e) {
            return buildErrorResponse(e, securityMode);
        } catch (IOException e) {
            return buildErrorResponse(new TransactionException(e), securityMode);
        } catch (ParseCancellationException e) {
            return buildErrorResponse(new InvalidURLException(e), securityMode);
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
        return this.get(path, queryParams, opaqueUser, SecurityMode.SECURITY_ACTIVE);
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
        RequestScope requestScope = null;
        try (DataStoreTransaction transaction = dataStore.beginTransaction()) {
            User user = transaction.accessUser(opaqueUser);
            JsonApiDocument doc = mapper.readJsonApiDocument(jsonApiDocument);
            requestScope = new RequestScope(doc,
                    transaction,
                    user,
                    dictionary,
                    mapper,
                    auditLogger,
                    securityMode,
                    permissionExecutor);
            PostVisitor visitor = new PostVisitor(requestScope);
            Supplier<Pair<Integer, JsonNode>> responder = visitor.visit(parse(path));
            requestScope.getPermissionExecutor().executeCommitChecks();
            requestScope.saveObjects();
            transaction.flush();
            ElideResponse response = buildResponse(responder.get());
            auditLogger.commit();
            transaction.commit();
            requestScope.runCommitTriggers();
            traceLogSecurityExceptions(requestScope);
            return response;
        } catch (ForbiddenAccessException e) {
            debugLogSecurityExceptions(requestScope);
            return buildErrorResponse(e, securityMode);
        } catch (HttpStatusException e) {
            return buildErrorResponse(e, securityMode);
        } catch (IOException e) {
            return buildErrorResponse(new TransactionException(e), securityMode);
        } catch (ParseCancellationException e) {
            return buildErrorResponse(new InvalidURLException(e), securityMode);
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
        return this.post(path, jsonApiDocument, opaqueUser, SecurityMode.SECURITY_ACTIVE);
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
        RequestScope requestScope = null;
        try (DataStoreTransaction transaction = dataStore.beginTransaction()) {
            User user = transaction.accessUser(opaqueUser);

            Supplier<Pair<Integer, JsonNode>> responder;
            if (JsonApiPatch.isPatchExtension(contentType) && JsonApiPatch.isPatchExtension(accept)) {
                // build Outer RequestScope to be used for each action
                PatchRequestScope patchRequestScope = new PatchRequestScope(
                        transaction, user, dictionary, mapper, auditLogger);
                requestScope = patchRequestScope;
                responder = JsonApiPatch.processJsonPatch(dataStore, path, jsonApiDocument, patchRequestScope);
            } else {
                JsonApiDocument doc = mapper.readJsonApiDocument(jsonApiDocument);
                requestScope = new RequestScope(doc, transaction, user, dictionary, mapper, auditLogger, securityMode,
                        permissionExecutor);
                PatchVisitor visitor = new PatchVisitor(requestScope);
                responder = visitor.visit(parse(path));
            }
            requestScope.getPermissionExecutor().executeCommitChecks();
            requestScope.saveObjects();
            transaction.flush();
            ElideResponse response = buildResponse(responder.get());
            auditLogger.commit();
            transaction.commit();
            requestScope.runCommitTriggers();
            traceLogSecurityExceptions(requestScope);
            return response;
        } catch (ForbiddenAccessException e) {
            debugLogSecurityExceptions(requestScope);
            return buildErrorResponse(e, securityMode);
        } catch (JsonPatchExtensionException e) {
            return buildResponse(e.getResponse());
        } catch (HttpStatusException e) {
            return buildErrorResponse(e, securityMode);
        } catch (ParseCancellationException e) {
            return buildErrorResponse(new InvalidURLException(e), securityMode);
        } catch (IOException e) {
            return buildErrorResponse(new TransactionException(e), securityMode);
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
        return this.patch(contentType, accept, path, jsonApiDocument, opaqueUser, SecurityMode.SECURITY_ACTIVE);
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
        RequestScope requestScope = null;
        try (DataStoreTransaction transaction = dataStore.beginTransaction()) {
            User user = transaction.accessUser(opaqueUser);
            if (jsonApiDocument != null && !jsonApiDocument.equals("")) {
                doc = mapper.readJsonApiDocument(jsonApiDocument);
            } else {
                doc = new JsonApiDocument();
            }
            requestScope = new RequestScope(
                    doc, transaction, user, dictionary, mapper, auditLogger, securityMode, permissionExecutor);
            DeleteVisitor visitor = new DeleteVisitor(requestScope);
            Supplier<Pair<Integer, JsonNode>> responder = visitor.visit(parse(path));
            requestScope.getPermissionExecutor().executeCommitChecks();
            requestScope.saveObjects();
            transaction.flush();
            ElideResponse response = buildResponse(responder.get());
            auditLogger.commit();
            transaction.commit();
            requestScope.runCommitTriggers();
            traceLogSecurityExceptions(requestScope);
            return response;
        } catch (ForbiddenAccessException e) {
            debugLogSecurityExceptions(requestScope);
            return buildErrorResponse(e, securityMode);
        } catch (HttpStatusException e) {
            return buildErrorResponse(e, securityMode);
        } catch (IOException e) {
            return buildErrorResponse(new TransactionException(e), securityMode);
        } catch (ParseCancellationException e) {
            return buildErrorResponse(new InvalidURLException(e), securityMode);
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
        return this.delete(path, jsonApiDocument, opaqueUser, SecurityMode.SECURITY_ACTIVE);
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

    protected void traceLogSecurityExceptions(RequestScope scope) {
        if (scope != null && log.isTraceEnabled())  {
            log.trace(scope.getAuthFailureReason());
        }
    }

    protected void debugLogSecurityExceptions(RequestScope scope) {
        if (scope != null && log.isDebugEnabled())  {
            log.debug(scope.getAuthFailureReason());
        }
    }

    protected ElideResponse buildErrorResponse(HttpStatusException error, SecurityMode securityMode) {
        return buildResponse(securityMode == SecurityMode.SECURITY_ACTIVE_VERBOSE
                ? error.getVerboseErrorResponse()
                : error.getErrorResponse()
        );
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
