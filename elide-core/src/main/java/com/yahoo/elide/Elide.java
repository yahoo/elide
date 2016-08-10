/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide;

import com.yahoo.elide.audit.AuditLogger;
import com.yahoo.elide.audit.Slf4jLogger;
import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.HttpStatus;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.exceptions.ForbiddenAccessException;
import com.yahoo.elide.core.exceptions.HttpStatusException;
import com.yahoo.elide.core.exceptions.InvalidURLException;
import com.yahoo.elide.core.exceptions.JsonPatchExtensionException;
import com.yahoo.elide.core.exceptions.TransactionException;
import com.yahoo.elide.core.filter.dialect.DefaultFilterDialect;
import com.yahoo.elide.core.filter.dialect.JoinFilterDialect;
import com.yahoo.elide.core.filter.dialect.MultipleFilterDialect;
import com.yahoo.elide.core.filter.dialect.SubqueryFilterDialect;
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
import com.yahoo.elide.security.PermissionExecutor;
import com.yahoo.elide.security.SecurityMode;
import com.yahoo.elide.security.User;
import com.yahoo.elide.security.executors.ActivePermissionExecutor;

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
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.ws.rs.core.MultivaluedMap;

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
    private final List<JoinFilterDialect> joinFilterDialects;
    private final List<SubqueryFilterDialect> subqueryFilterDialects;
    private final boolean useFilterExpressions;

    /**
     * Instantiates a new Elide.
     *
     * @param auditLogger the audit logger
     * @param dataStore the dataStore
     * @param dictionary the dictionary
     * @deprecated Since 2.1, use the {@link Elide.Builder} instead
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
     * @deprecated Since 2.1, use the {@link Elide.Builder} instead
     */
    @Deprecated
    public Elide(AuditLogger auditLogger, DataStore dataStore) {
        this(auditLogger, dataStore, new EntityDictionary(new HashMap<>()));
    }

    /**
     * Instantiates a new Elide.
     *
     * @param auditLogger the audit logger
     * @param dataStore the dataStore
     * @param dictionary the dictionary
     * @param mapper Serializer/Deserializer for JSON API
     * @deprecated Since 2.1, use the {@link Elide.Builder} instead
     */
    @Deprecated
    public Elide(AuditLogger auditLogger, DataStore dataStore, EntityDictionary dictionary, JsonApiMapper mapper) {
        this(
                auditLogger,
                dataStore,
                dictionary,
                mapper,
                ActivePermissionExecutor::new,
                Collections.singletonList(new DefaultFilterDialect(dictionary)),
                Collections.singletonList(new DefaultFilterDialect(dictionary)),
                false
        );
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
    protected Elide(AuditLogger auditLogger,
                  DataStore dataStore,
                  EntityDictionary dictionary,
                  JsonApiMapper mapper,
                  Function<RequestScope, PermissionExecutor> permissionExecutor) {
        this(
            auditLogger,
            dataStore,
            dictionary,
            mapper,
            ActivePermissionExecutor::new,
            Collections.singletonList(new DefaultFilterDialect(dictionary)),
            Collections.singletonList(new DefaultFilterDialect(dictionary)),
            false
        );
    }

    /**
     * Instantiates a new Elide.
     *
     * @param auditLogger the audit logger
     * @param dataStore the dataStore
     * @param dictionary the dictionary
     * @param mapper Serializer/Deserializer for JSON API
     * @param permissionExecutor Custom permission executor implementation
     * @param joinFilterDialects A list of filter parsers to use for filtering across types
     * @param subqueryFilterDialects A list of filter parsers to use for filtering by type
     * @param useFilterExpressions Whether or not to use Elide 3.0 filter expressions for DataStore interactions
     */
    protected Elide(AuditLogger auditLogger,
                  DataStore dataStore,
                  EntityDictionary dictionary,
                  JsonApiMapper mapper,
                  Function<RequestScope, PermissionExecutor> permissionExecutor,
                  List<JoinFilterDialect> joinFilterDialects,
                  List<SubqueryFilterDialect> subqueryFilterDialects,
                  boolean useFilterExpressions) {
        this.auditLogger = auditLogger;
        this.dataStore = dataStore;
        this.dictionary = dictionary;
        dataStore.populateEntityDictionary(dictionary);
        this.mapper = mapper;
        this.permissionExecutor = permissionExecutor;
        this.joinFilterDialects = joinFilterDialects;
        this.subqueryFilterDialects = subqueryFilterDialects;
        this.useFilterExpressions = useFilterExpressions;
    }

    /**
     * Elide Builder for constructing an Elide instance.
     */
    public static class Builder {
        private final DataStore dataStore;
        private AuditLogger auditLogger;
        private JsonApiMapper jsonApiMapper;
        private EntityDictionary entityDictionary = new EntityDictionary(new HashMap<>());
        private Function<RequestScope, PermissionExecutor> permissionExecutorFunction = ActivePermissionExecutor::new;
        private List<JoinFilterDialect> joinFilterDialects;
        private List<SubqueryFilterDialect> subqueryFilterDialects;
        private boolean useFilterExpressions;

        /**
         * A new builder used to generate Elide instances. Instantiates an {@link EntityDictionary} without
         * providing a mapping of security checks.
         *
         * @param auditLogger the logger to use for audit annotations
         * @param dataStore the datastore used to communicate with the persistence layer
         * @deprecated 2.3 use {@link #Builder(DataStore)}
         */
        public Builder(AuditLogger auditLogger, DataStore dataStore) {
            this.auditLogger = auditLogger;
            this.dataStore = dataStore;
            this.jsonApiMapper = new JsonApiMapper(entityDictionary);
            this.joinFilterDialects = new ArrayList<>();
            this.subqueryFilterDialects = new ArrayList<>();

        }

        /**
         * A new builder used to generate Elide instances. Instantiates an {@link EntityDictionary} without
         * providing a mapping of security checks and uses the provided {@link Slf4jLogger} for audit.
         *
         * @param dataStore the datastore used to communicate with the persistence layer
         */
        public Builder(DataStore dataStore) {
            this.dataStore = dataStore;
            this.auditLogger = new Slf4jLogger();
            this.jsonApiMapper = new JsonApiMapper(entityDictionary);
            this.joinFilterDialects = new ArrayList<>();
            this.subqueryFilterDialects = new ArrayList<>();
        }

        public Elide build() {
            if (joinFilterDialects.isEmpty()) {
                joinFilterDialects.add(new DefaultFilterDialect(entityDictionary));
            }

            if (subqueryFilterDialects.isEmpty()) {
                subqueryFilterDialects.add(new DefaultFilterDialect(entityDictionary));
            }

            return new Elide(
                    auditLogger,
                    dataStore,
                    entityDictionary,
                    jsonApiMapper,
                    permissionExecutorFunction,
                    joinFilterDialects,
                    subqueryFilterDialects,
                    useFilterExpressions);
        }

        @Deprecated
        public Builder auditLogger(final AuditLogger auditLogger) {
            return withAuditLogger(auditLogger);
        }

        @Deprecated
        public Builder entityDictionary(final EntityDictionary entityDictionary) {
            return withEntityDictionary(entityDictionary);
        }

        @Deprecated
        public Builder jsonApiMapper(final JsonApiMapper jsonApiMapper) {
            return withJsonApiMapper(jsonApiMapper);
        }

        @Deprecated
        public Builder permissionExecutor(final Function<RequestScope, PermissionExecutor> permissionExecutorFunction) {
            return withPermissionExecutor(permissionExecutorFunction);
        }

        @Deprecated
        public Builder permissionExecutor(final Class<? extends PermissionExecutor> permissionExecutorClass) {
            return withPermissionExecutor(permissionExecutorClass);
        }

        public Builder withAuditLogger(AuditLogger auditLogger) {
            this.auditLogger = auditLogger;
            return this;
        }

        public Builder withEntityDictionary(EntityDictionary entityDictionary) {
            this.entityDictionary = entityDictionary;
            return this;
        }

        public Builder withJsonApiMapper(JsonApiMapper jsonApiMapper) {
            this.jsonApiMapper = jsonApiMapper;
            return this;
        }

        public Builder withPermissionExecutor(Function<RequestScope, PermissionExecutor> permissionExecutorFunction) {
            this.permissionExecutorFunction = permissionExecutorFunction;
            return this;
        }

        public Builder withPermissionExecutor(Class<? extends PermissionExecutor> permissionExecutorClass) {
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

        public Builder withJoinFilterDialect(JoinFilterDialect dialect) {
            useFilterExpressions = true;
            joinFilterDialects.add(dialect);
            return this;
        }

        public Builder withSubqueryFilterDialect(SubqueryFilterDialect dialect) {
            useFilterExpressions = true;
            subqueryFilterDialects.add(dialect);
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
     * @deprecated Since 2.1, instead use the {@link Elide.Builder} with an appropriate {@link PermissionExecutor}
     */
    @Deprecated
    public ElideResponse get(
            String path,
            MultivaluedMap<String, String> queryParams,
            Object opaqueUser,
            SecurityMode securityMode) {

        RequestScope requestScope = null;
        boolean isVerbose = false;
        try (DataStoreTransaction transaction = dataStore.beginReadTransaction()) {
            final User user = transaction.accessUser(opaqueUser);
            requestScope = new RequestScope(
                    path,
                    new JsonApiDocument(),
                    transaction,
                    user,
                    dictionary,
                    mapper,
                    auditLogger,
                    queryParams,
                    securityMode,
                    permissionExecutor,
                    new MultipleFilterDialect(joinFilterDialects, subqueryFilterDialects),
                    useFilterExpressions);

            isVerbose = requestScope.getPermissionExecutor().isVerbose();
            GetVisitor visitor = new GetVisitor(requestScope);
            Supplier<Pair<Integer, JsonNode>> responder = visitor.visit(parse(path));
            transaction.preCommit();
            requestScope.getPermissionExecutor().executeCommitChecks();
            transaction.flush();
            ElideResponse response = buildResponse(responder.get());
            auditLogger.commit();
            transaction.commit();
            requestScope.runCommitTriggers();
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
     * @deprecated Since 2.1, instead use the {@link Elide.Builder} with an appropriate {@link PermissionExecutor}
     */
    @Deprecated
    public ElideResponse post(
            String path,
            String jsonApiDocument,
            Object opaqueUser,
            SecurityMode securityMode) {
        RequestScope requestScope = null;
        boolean isVerbose = false;
        try (DataStoreTransaction transaction = dataStore.beginTransaction()) {
            User user = transaction.accessUser(opaqueUser);
            JsonApiDocument doc = mapper.readJsonApiDocument(jsonApiDocument);
            requestScope = new RequestScope(path, doc,
                    transaction,
                    user,
                    dictionary,
                    mapper,
                    auditLogger,
                    securityMode,
                    permissionExecutor);
            isVerbose = requestScope.getPermissionExecutor().isVerbose();
            PostVisitor visitor = new PostVisitor(requestScope);
            Supplier<Pair<Integer, JsonNode>> responder = visitor.visit(parse(path));
            transaction.preCommit();
            requestScope.getPermissionExecutor().executeCommitChecks();
            requestScope.saveOrCreateObjects();
            transaction.flush();
            ElideResponse response = buildResponse(responder.get());
            auditLogger.commit();
            transaction.commit();
            requestScope.runCommitTriggers();
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
     * @deprecated Since 2.1, instead use the {@link Elide.Builder} with an appropriate {@link PermissionExecutor}
     */
    @Deprecated
    public ElideResponse patch(
            String contentType,
            String accept,
            String path,
            String jsonApiDocument,
            Object opaqueUser,
            SecurityMode securityMode) {
        RequestScope requestScope = null;
        boolean isVerbose = false;
        try (DataStoreTransaction transaction = dataStore.beginTransaction()) {
            User user = transaction.accessUser(opaqueUser);

            Supplier<Pair<Integer, JsonNode>> responder;
            if (JsonApiPatch.isPatchExtension(contentType) && JsonApiPatch.isPatchExtension(accept)) {
                // build Outer RequestScope to be used for each action
                PatchRequestScope patchRequestScope = new PatchRequestScope(path,
                        transaction, user, dictionary, mapper, auditLogger, permissionExecutor);
                requestScope = patchRequestScope;
                isVerbose = requestScope.getPermissionExecutor().isVerbose();
                responder = JsonApiPatch.processJsonPatch(dataStore, path, jsonApiDocument, patchRequestScope);
            } else {
                JsonApiDocument doc = mapper.readJsonApiDocument(jsonApiDocument);
                requestScope = new RequestScope(path, doc, transaction, user, dictionary, mapper, auditLogger,
                        securityMode, permissionExecutor);
                isVerbose = requestScope.getPermissionExecutor().isVerbose();
                PatchVisitor visitor = new PatchVisitor(requestScope);
                responder = visitor.visit(parse(path));
            }
            transaction.preCommit();
            requestScope.getPermissionExecutor().executeCommitChecks();
            requestScope.saveOrCreateObjects();
            transaction.flush();
            ElideResponse response = buildResponse(responder.get());
            auditLogger.commit();
            transaction.commit();
            requestScope.runCommitTriggers();
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
     * @deprecated Since 2.1, instead use the {@link Elide.Builder} with an appropriate {@link PermissionExecutor}
     */
    @Deprecated
    public ElideResponse delete(
            String path,
            String jsonApiDocument,
            Object opaqueUser,
            SecurityMode securityMode) {
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
                    path, doc, transaction, user, dictionary, mapper, auditLogger, securityMode, permissionExecutor);
            isVerbose = requestScope.getPermissionExecutor().isVerbose();
            DeleteVisitor visitor = new DeleteVisitor(requestScope);
            Supplier<Pair<Integer, JsonNode>> responder = visitor.visit(parse(path));
            transaction.preCommit();
            requestScope.getPermissionExecutor().executeCommitChecks();
            requestScope.saveOrCreateObjects();
            transaction.flush();
            ElideResponse response = buildResponse(responder.get());
            auditLogger.commit();
            transaction.commit();
            requestScope.runCommitTriggers();
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
