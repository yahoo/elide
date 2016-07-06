/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

import com.yahoo.elide.annotation.OnCommit;
import com.yahoo.elide.audit.AuditLogger;
import com.yahoo.elide.core.exceptions.InvalidPredicateException;
import com.yahoo.elide.core.filter.dialect.MultipleFilterDialect;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.dialect.ParseException;
import com.yahoo.elide.core.pagination.Pagination;
import com.yahoo.elide.core.sort.Sorting;
import com.yahoo.elide.jsonapi.JsonApiMapper;
import com.yahoo.elide.jsonapi.models.JsonApiDocument;
import com.yahoo.elide.security.PermissionExecutor;
import com.yahoo.elide.security.SecurityMode;
import com.yahoo.elide.security.User;
import com.yahoo.elide.security.executors.ActivePermissionExecutor;
import lombok.Getter;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * Request scope object for relaying request-related data to various subsystems.
 */
public class RequestScope implements com.yahoo.elide.security.RequestScope {
    @Getter private final JsonApiDocument jsonApiDocument;
    @Getter private final DataStoreTransaction transaction;
    @Getter private final User user;
    @Getter private final EntityDictionary dictionary;
    @Getter private final JsonApiMapper mapper;
    @Getter private final AuditLogger auditLogger;
    @Getter private final Optional<MultivaluedMap<String, String>> queryParams;
    @Getter private final Map<String, Set<String>> sparseFields;
    @Getter private final Pagination pagination;
    @Getter private final Sorting sorting;
    @Getter private final SecurityMode securityMode;
    @Getter private final PermissionExecutor permissionExecutor;
    @Getter private final ObjectEntityCache objectEntityCache;
    @Getter private final Set<PersistentResource> newPersistentResources;
    @Getter private final LinkedHashSet<PersistentResource> dirtyResources;
    @Getter private final String path;
    private final boolean useFilterExpressions;
    private final MultipleFilterDialect filterDialect;
    private final Map<String, FilterExpression> expressionsByType;

    /* Used to filter across heterogeneous types during the first load */
    private FilterExpression globalFilterExpression;

    final private transient LinkedHashSet<Runnable> commitTriggers;

    /**
     * Create a new RequestScope.
     * @param path the URL path
     * @param jsonApiDocument the document for this request
     * @param transaction the transaction for this request
     * @param user the user making this request
     * @param dictionary the entity dictionary
     * @param mapper converts JsonApiDocuments to raw JSON
     * @param auditLogger logger for this request
     * @param queryParams the query parameters
     * @param securityMode the current security mode
     * @param permissionExecutorGenerator the user-provided function that will generate a permissionExecutor
     */
    public RequestScope(String path,
                        JsonApiDocument jsonApiDocument,
                        DataStoreTransaction transaction,
                        User user,
                        EntityDictionary dictionary,
                        JsonApiMapper mapper,
                        AuditLogger auditLogger,
                        MultivaluedMap<String, String> queryParams,
                        SecurityMode securityMode,
                        Function<RequestScope, PermissionExecutor> permissionExecutorGenerator,
                        MultipleFilterDialect filterDialect,
                        boolean useFilterExpressions) {
        this.path = path;
        this.jsonApiDocument = jsonApiDocument;
        this.transaction = transaction;
        this.user = user;
        this.dictionary = dictionary;
        this.mapper = mapper;
        this.auditLogger = auditLogger;
        this.securityMode = securityMode;
        this.filterDialect = filterDialect;
        this.useFilterExpressions = useFilterExpressions;

        this.globalFilterExpression = null;
        this.expressionsByType = new HashMap<>();
        this.objectEntityCache = new ObjectEntityCache();
        this.newPersistentResources = new LinkedHashSet<>();
        this.dirtyResources = new LinkedHashSet<>();
        this.commitTriggers = new LinkedHashSet<>();

        this.permissionExecutor = (permissionExecutorGenerator == null)
                ? new ActivePermissionExecutor(this)
                : permissionExecutorGenerator.apply(this);

        this.queryParams = (queryParams == null || queryParams.size() == 0)
                ? Optional.empty()
                : Optional.of(queryParams);

        if (this.queryParams.isPresent()) {

            /* Extract any query param that starts with 'filter' */
            MultivaluedMap filterParams = getFilterParams(queryParams);

            String errorMessage = "";
            if (! filterParams.isEmpty()) {

                /* First check to see if there is a global, cross-type filter */
                try {
                    globalFilterExpression = filterDialect.parseGlobalExpression(path, filterParams);
                } catch (ParseException e) {
                    errorMessage = e.getMessage();
                }

                /* Next check to see if there is are type specific filters */
                try {
                    expressionsByType.putAll(filterDialect.parseTypedExpression(path, filterParams));
                } catch (ParseException e) {

                    /* If neither dialect parsed, report the last error found */
                    if (globalFilterExpression == null) {

                        if (errorMessage.isEmpty()) {
                            errorMessage = e.getMessage();
                        } else if (! errorMessage.equals(e.getMessage())) {

                            /* Combine the two different messages together */
                            errorMessage = errorMessage + "\n" + e.getMessage();
                        }

                        throw new InvalidPredicateException(errorMessage);
                    }
                }
            }

            this.sparseFields = parseSparseFields(queryParams);
            this.sorting = Sorting.parseQueryParams(queryParams);
            this.pagination = Pagination.parseQueryParams(queryParams);
        } else {
            this.sparseFields = Collections.emptyMap();
            this.sorting = Sorting.getDefaultEmptyInstance();
            this.pagination = Pagination.getDefaultPagination();
        }

        if (transaction instanceof RequestScopedTransaction) {
            ((RequestScopedTransaction) transaction).setRequestScope(this);
        }
    }

    public RequestScope(String path,
                        JsonApiDocument jsonApiDocument,
                        DataStoreTransaction transaction,
                        User user,
                        EntityDictionary dictionary,
                        JsonApiMapper mapper,
                        AuditLogger auditLogger,
                        SecurityMode securityMode,
                        Function<RequestScope, PermissionExecutor> permissionExecutor) {
        this(
                path,
                jsonApiDocument,
                transaction,
                user,
                dictionary,
                mapper,
                auditLogger,
                null,
                securityMode,
                permissionExecutor,
                new MultipleFilterDialect(dictionary),
                false
        );
    }

    public RequestScope(String path,
                        JsonApiDocument jsonApiDocument,
                        DataStoreTransaction transaction,
                        User user,
                        EntityDictionary dictionary,
                        JsonApiMapper mapper,
                        AuditLogger auditLogger,
                        MultivaluedMap<String, String> queryParams) {
        this(
                path,
                jsonApiDocument,
                transaction,
                user,
                dictionary,
                mapper,
                auditLogger,
                queryParams,
                SecurityMode.SECURITY_ACTIVE,
                null,
                new MultipleFilterDialect(dictionary),
                false
        );
    }

    public RequestScope(String path,
                        JsonApiDocument jsonApiDocument,
                        DataStoreTransaction transaction,
                        User user,
                        EntityDictionary dictionary,
                        JsonApiMapper mapper,
                        AuditLogger auditLogger) {
        this(
                path,
                jsonApiDocument,
                transaction,
                user,
                dictionary,
                mapper,
                auditLogger,
                null,
                SecurityMode.SECURITY_ACTIVE,
                null,
                new MultipleFilterDialect(dictionary),
                false
        );
    }

    /**
     * Outer RequestScope constructor for use by Patch Extension.
     *
     * @param transaction the transaction
     * @param user        the user
     * @param dictionary  the dictionary
     * @param mapper      the mapper
     * @param auditLogger      the logger
     */
    protected RequestScope(DataStoreTransaction transaction,
                           User user,
                           EntityDictionary dictionary,
                           JsonApiMapper mapper,
                           AuditLogger auditLogger) {
        this(null, null, transaction, user, dictionary, mapper, auditLogger);
    }

    /**
     * Special copy constructor for use by PatchRequestScope.
     *
     * @param jsonApiDocument   the json api document
     * @param outerRequestScope the outer request scope
     */
    protected RequestScope(String path, JsonApiDocument jsonApiDocument, RequestScope outerRequestScope) {
        this.jsonApiDocument = jsonApiDocument;
        this.path = path;
        this.transaction = outerRequestScope.transaction;
        this.user = outerRequestScope.user;
        this.dictionary = outerRequestScope.dictionary;
        this.mapper = outerRequestScope.mapper;
        this.auditLogger = outerRequestScope.auditLogger;
        this.queryParams = Optional.empty();
        this.sparseFields = Collections.emptyMap();
        this.sorting = Sorting.getDefaultEmptyInstance();
        this.pagination = Pagination.getDefaultPagination();
        this.objectEntityCache = outerRequestScope.objectEntityCache;
        this.securityMode = outerRequestScope.securityMode;
        this.newPersistentResources = outerRequestScope.newPersistentResources;
        this.commitTriggers = outerRequestScope.commitTriggers;
        this.permissionExecutor = outerRequestScope.getPermissionExecutor();
        this.dirtyResources = outerRequestScope.dirtyResources;
        this.filterDialect = outerRequestScope.filterDialect;
        this.expressionsByType = outerRequestScope.expressionsByType;
        this.useFilterExpressions = outerRequestScope.useFilterExpressions;
    }

    public Set<com.yahoo.elide.security.PersistentResource> getNewResources() {
        return (Set<com.yahoo.elide.security.PersistentResource>) (Set) newPersistentResources;
    }

    /**
     * Parses queryParams and produces sparseFields map.
     * @param queryParams The request query parameters
     * @return Parsed sparseFields map
     */
    private static Map<String, Set<String>> parseSparseFields(MultivaluedMap<String, String> queryParams) {
        Map<String, Set<String>> result = new HashMap<>();

        for (Map.Entry<String, List<String>> kv : queryParams.entrySet()) {
            String key = kv.getKey();
            if (key.startsWith("fields[") && key.endsWith("]")) {
                String type = key.substring(7, key.length() - 1);

                LinkedHashSet<String> filters = new LinkedHashSet<>();
                for (String filterParams : kv.getValue()) {
                    Collections.addAll(filters, filterParams.split(","));
                }

                if (!filters.isEmpty()) {
                    result.put(type, filters);
                }
            }
        }

        return result;
    }

    /**
     * Get filter expression for a specific collection type.
     * @param type The name of the type
     * @return The filter expression for the given type
     */
    public Optional<FilterExpression> getFilterExpressionByType(String type) {
        return Optional.ofNullable(expressionsByType.get(type));
    }

    /**
     * Get the global/cross-type filter expression.
     * @param loadClass
     * @return The global filter expression evaluated at the first load
     */
    public Optional<FilterExpression> getLoadFilterExpression(Class<?> loadClass) {
        if (globalFilterExpression == null) {
            String typeName = dictionary.getJsonAliasFor(loadClass);
            return getFilterExpressionByType(typeName);
        }
        return Optional.of(globalFilterExpression);
    }

    /**
     * Extracts any query params that start with 'filter'.
     * @param queryParams
     * @return extracted filter params
     */
    private static MultivaluedMap<String, String> getFilterParams(MultivaluedMap<String, String> queryParams) {
        MultivaluedMap<String, String> returnMap = new MultivaluedHashMap<>();

        queryParams.entrySet()
                .stream()
                .filter((entry) -> entry.getKey().startsWith("filter"))
                .forEach((entry) -> {
                    returnMap.put(entry.getKey(), entry.getValue());
                });
        return returnMap;
    }

    /**
     * run any deferred post-commit triggers.
     *
     * @see com.yahoo.elide.annotation.CreatePermission
     */
    public void runCommitTriggers() {
        new ArrayList<>(commitTriggers).forEach(Runnable::run);
        commitTriggers.clear();
    }

    public void queueCommitTrigger(PersistentResource resource) {
        queueCommitTrigger(resource, "");
    }

    public void queueCommitTrigger(PersistentResource resource, String fieldName) {
        commitTriggers.add(() -> resource.runTriggers(OnCommit.class, fieldName));
    }

    public void saveObjects() {
        dirtyResources.stream().map(PersistentResource::getObject).forEach(transaction::save);
    }

    /**
     * Whether or not to use Elide 3.0 filter expressions for DataStoreTransaction calls
     * @return
     */
    public boolean useFilterExpressions() {
        return useFilterExpressions;
    }
}
