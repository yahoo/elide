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
import com.yahoo.elide.core.pagination.Pagination;
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
    private final ElideSettings elideSettings;

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
     */
    protected Elide(AuditLogger auditLogger,
                    DataStore dataStore,
                    EntityDictionary dictionary,
                    JsonApiMapper mapper,
                    Function<RequestScope, PermissionExecutor> permissionExecutor,
                    List<JoinFilterDialect> joinFilterDialects,
                    List<SubqueryFilterDialect> subqueryFilterDialects,
                    int maxDefaultPageSize,
                    int defaultPageSize,
                    boolean useFilterExpressions,
                    int updateStatusCode) {
        this.auditLogger = auditLogger;
        this.dataStore = dataStore;
        this.dictionary = dictionary;
        dataStore.populateEntityDictionary(dictionary);
        this.mapper = mapper;
        this.permissionExecutor = permissionExecutor;
        this.joinFilterDialects = joinFilterDialects;
        this.subqueryFilterDialects = subqueryFilterDialects;
        this.elideSettings = new ElideSettings(maxDefaultPageSize,
                defaultPageSize, useFilterExpressions, updateStatusCode);
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
        private int defaultMaxPageSize = Pagination.MAX_PAGE_LIMIT;
        private int defaultPageSize = Pagination.DEFAULT_PAGE_LIMIT;
        private boolean useFilterExpressions;
        private int updateStatusCode;

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
            updateStatusCode = HttpStatus.SC_NO_CONTENT;
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
            updateStatusCode = HttpStatus.SC_NO_CONTENT;
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
                    defaultMaxPageSize,
                    defaultPageSize,
                    useFilterExpressions,
                    updateStatusCode);
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
            joinFilterDialects.add(dialect);
            return this;
        }

        public Builder withSubqueryFilterDialect(SubqueryFilterDialect dialect) {
            subqueryFilterDialects.add(dialect);
            return this;
        }

        public Builder withDefaultMaxPageSize(int maxPageSize) {
            defaultMaxPageSize = maxPageSize;
            return this;
        }

        public Builder withDefaultPageSize(int pageSize) {
            defaultPageSize = pageSize;
            return this;
        }

        public Builder withUpdate200Status() {
            updateStatusCode = HttpStatus.SC_OK;
            return this;
        }

        public Builder withUpdate204Status() {
            updateStatusCode = HttpStatus.SC_NO_CONTENT;
            return this;
        }

        public Builder withUseFilterExpressions(boolean useFilterExpressions) {
            this.useFilterExpressions = useFilterExpressions;
            return this;
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
                    permissionExecutor,
                    elideSettings,
                    new MultipleFilterDialect(joinFilterDialects, subqueryFilterDialects));

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
                    dictionary,
                    mapper,
                    auditLogger,
                    null,
                    permissionExecutor,
                    elideSettings,
                    new MultipleFilterDialect(joinFilterDialects, subqueryFilterDialects));
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
                        dictionary,
                        mapper,
                        auditLogger,
                        permissionExecutor,
                        elideSettings,
                        new MultipleFilterDialect(joinFilterDialects, subqueryFilterDialects));
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
                        dictionary,
                        mapper,
                        auditLogger,
                        (MultivaluedMap) null,
                        permissionExecutor,
                        elideSettings,
                        new MultipleFilterDialect(joinFilterDialects, subqueryFilterDialects));
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
                    dictionary,
                    mapper,
                    auditLogger,
                    null,
                    permissionExecutor,
                    elideSettings,
                    new MultipleFilterDialect(joinFilterDialects, subqueryFilterDialects));
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

    /**
     * Object containing general Elide settings passed to RequestScope.
     */
    public static class ElideSettings {
        public final int defaultMaxPageSize;
        public final int defaultPageSize;
        public final boolean useFilterExpressions;
        public final int updateStatusCode;

        public ElideSettings(int defaultMaxPageSize, int defaultPageSize) {
            this(defaultMaxPageSize, defaultPageSize, true, HttpStatus.SC_NO_CONTENT);
        }

        public ElideSettings(int defaultMaxPageSize,
                             int defaultPageSize,
                             boolean useFilterExpressions,
                             int updateStatusCode
                             ) {
            this.defaultMaxPageSize = defaultMaxPageSize;
            this.defaultPageSize = defaultPageSize;
            this.useFilterExpressions = useFilterExpressions;
            this.updateStatusCode = updateStatusCode;
        }
    }
}
