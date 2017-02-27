/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

import com.yahoo.elide.Elide;
import com.yahoo.elide.annotation.OnCreatePreCommit;
import com.yahoo.elide.annotation.OnCreatePreSecurity;
import com.yahoo.elide.annotation.OnCreatePostCommit;
import com.yahoo.elide.annotation.OnDeletePreSecurity;
import com.yahoo.elide.annotation.OnReadPostCommit;
import com.yahoo.elide.annotation.OnReadPreCommit;
import com.yahoo.elide.annotation.OnReadPreSecurity;
import com.yahoo.elide.annotation.OnUpdatePostCommit;
import com.yahoo.elide.annotation.OnUpdatePreSecurity;
import com.yahoo.elide.annotation.OnDeletePostCommit;
import com.yahoo.elide.annotation.OnDeletePreCommit;
import com.yahoo.elide.annotation.OnUpdatePreCommit;
import com.yahoo.elide.audit.AuditLogger;
import com.yahoo.elide.core.exceptions.InternalServerErrorException;
import com.yahoo.elide.core.exceptions.InvalidPredicateException;
import com.yahoo.elide.core.filter.dialect.MultipleFilterDialect;
import com.yahoo.elide.core.filter.dialect.ParseException;
import com.yahoo.elide.core.filter.expression.AndFilterExpression;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.pagination.Pagination;
import com.yahoo.elide.core.sort.Sorting;
import com.yahoo.elide.jsonapi.JsonApiMapper;
import com.yahoo.elide.jsonapi.models.JsonApiDocument;
import com.yahoo.elide.security.PermissionExecutor;
import com.yahoo.elide.security.User;
import com.yahoo.elide.security.executors.ActivePermissionExecutor;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

/**
 * Request scope object for relaying request-related data to various subsystems.
 */
@Slf4j
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
    @Getter private final PermissionExecutor permissionExecutor;
    @Getter private final ObjectEntityCache objectEntityCache;
    @Getter private final Set<PersistentResource> newPersistentResources;
    @Getter private final LinkedHashSet<PersistentResource> dirtyResources;
    @Getter private final String path;
    @Getter private final Elide.ElideSettings elideSettings;
    @Getter private int updateStatusCode;
    private final boolean useFilterExpressions;

    private final MultipleFilterDialect filterDialect;
    private final Map<String, FilterExpression> expressionsByType;

    /* Used to filter across heterogeneous types during the first load */
    private FilterExpression globalFilterExpression;

    final private transient HashMap<Class, LinkedHashSet<Runnable>> queuedTriggers;

    /**
     * Create a new RequestScope with specified update status code
     *
     * @param path the URL path
     * @param jsonApiDocument the document for this request
     * @param transaction the transaction for this request
     * @param user the user making this request
     * @param dictionary the entity dictionary
     * @param mapper converts JsonApiDocuments to raw JSON
     * @param auditLogger logger for this request
     * @param queryParams the query parameters
     * @param permissionExecutorGenerator the user-provided function that will generate a permissionExecutor
     * @param elideSettings Elide settings object
     * @param filterDialect Filter dialect
     */
    public RequestScope(String path,
                        JsonApiDocument jsonApiDocument,
                        DataStoreTransaction transaction,
                        User user,
                        EntityDictionary dictionary,
                        JsonApiMapper mapper,
                        AuditLogger auditLogger,
                        MultivaluedMap<String, String> queryParams,
                        Function<RequestScope, PermissionExecutor> permissionExecutorGenerator,
                        Elide.ElideSettings elideSettings,
                        MultipleFilterDialect filterDialect) {
        this.path = path;
        this.jsonApiDocument = jsonApiDocument;
        this.transaction = transaction;
        this.user = user;
        this.dictionary = dictionary;
        this.mapper = mapper;
        this.auditLogger = auditLogger;
        this.filterDialect = filterDialect;
        this.elideSettings = elideSettings;
        this.useFilterExpressions = elideSettings.useFilterExpressions;
        this.updateStatusCode = elideSettings.updateStatusCode;

        this.globalFilterExpression = null;
        this.expressionsByType = new HashMap<>();
        this.objectEntityCache = new ObjectEntityCache();
        this.newPersistentResources = new LinkedHashSet<>();
        this.dirtyResources = new LinkedHashSet<>();
        this.queuedTriggers = new HashMap<Class, LinkedHashSet<Runnable>>() {
            {
                put(OnCreatePreSecurity.class, new LinkedHashSet<>());
                put(OnUpdatePreSecurity.class, new LinkedHashSet<>());
                put(OnDeletePreSecurity.class, new LinkedHashSet<>());
                put(OnReadPreSecurity.class, new LinkedHashSet<>());
                put(OnCreatePreCommit.class, new LinkedHashSet<>());
                put(OnUpdatePreCommit.class, new LinkedHashSet<>());
                put(OnDeletePreCommit.class, new LinkedHashSet<>());
                put(OnReadPreCommit.class, new LinkedHashSet<>());
                put(OnCreatePostCommit.class, new LinkedHashSet<>());
                put(OnUpdatePostCommit.class, new LinkedHashSet<>());
                put(OnDeletePostCommit.class, new LinkedHashSet<>());
                put(OnReadPostCommit.class, new LinkedHashSet<>());
            }
        };

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
            this.pagination = Pagination.parseQueryParams(queryParams, this.getElideSettings());
        } else {
            this.sparseFields = Collections.emptyMap();
            this.sorting = Sorting.getDefaultEmptyInstance();
            this.pagination = Pagination.getDefaultPagination(this.getElideSettings());
        }
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
        this.pagination = Pagination.getDefaultPagination(outerRequestScope.getElideSettings());
        this.objectEntityCache = outerRequestScope.objectEntityCache;
        this.newPersistentResources = outerRequestScope.newPersistentResources;
        this.queuedTriggers = outerRequestScope.queuedTriggers;
        this.permissionExecutor = outerRequestScope.getPermissionExecutor();
        this.dirtyResources = outerRequestScope.dirtyResources;
        this.filterDialect = outerRequestScope.filterDialect;
        this.expressionsByType = outerRequestScope.expressionsByType;
        this.elideSettings = outerRequestScope.elideSettings;
        this.useFilterExpressions = outerRequestScope.useFilterExpressions;
        this.updateStatusCode = outerRequestScope.updateStatusCode;
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
        Optional<FilterExpression> permissionFilter;
        permissionFilter = getPermissionExecutor().getReadPermissionFilter(loadClass);
        Optional<FilterExpression> globalFilterExpressionOptional = null;
        if (globalFilterExpression == null) {
            String typeName = dictionary.getJsonAliasFor(loadClass);
            globalFilterExpressionOptional =  getFilterExpressionByType(typeName);
        } else {
            globalFilterExpressionOptional = Optional.of(globalFilterExpression);
        }

        if (globalFilterExpressionOptional.isPresent() && permissionFilter.isPresent()) {
            return Optional.of(new AndFilterExpression(globalFilterExpressionOptional.get(),
                    permissionFilter.get()));
        }
        else if (globalFilterExpressionOptional.isPresent()) {
            return globalFilterExpressionOptional;
        }
        else if (permissionFilter.isPresent()) {
            return permissionFilter;
        } else {
            return Optional.empty();
        }
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
     * Run queued on triggers (i.e. @OnCreatePreSecurity, @OnUpdatePreSecurity, etc.)
     */
    public void runQueuedPreSecurityTriggers() {
        runQueuedTriggers(OnCreatePreSecurity.class);
        runQueuedTriggers(OnUpdatePreSecurity.class);
        runQueuedTriggers(OnDeletePreSecurity.class);
    }

    /**
     * Run queued pre triggers (i.e. @OnCreatePreCommit, @OnUpdatePreCommit, etc.)
     */
    public void runQueuedPreCommitTriggers() {
        runQueuedTriggers(OnCreatePreCommit.class);
        runQueuedTriggers(OnUpdatePreCommit.class);
        runQueuedTriggers(OnDeletePreCommit.class);
        runQueuedTriggers(OnReadPreCommit.class);
    }

    /**
     * Run queued post triggers (i.e. @OnCreatePostCommit, @OnUpdatePostCommit, etc.)
     */
    public void runQueuedPostCommitTriggers() {
        runQueuedTriggers(OnCreatePostCommit.class);
        runQueuedTriggers(OnUpdatePostCommit.class);
        runQueuedTriggers(OnDeletePostCommit.class);
        runQueuedTriggers(OnReadPostCommit.class);
    }

    /**
     * Run any queued triggers for a specific type
     *
     * @param triggerType Class representing the trigger type (i.e. OnCreatePreSecurity.class, etc.)
     */
    private void runQueuedTriggers(Class triggerType) {
        if (!queuedTriggers.containsKey(triggerType)) {
            // NOTE: This is a programming error. Should never occur.
            throw new InternalServerErrorException("Failed to run queued trigger of type: " + triggerType);
        }
        LinkedHashSet<Runnable> triggers = queuedTriggers.get(triggerType);
        triggers.forEach(Runnable::run);
        triggers.clear();
    }

    /**
     * Queue a trigger for a particular resource
     *
     * @param resource Resource on which to execute trigger
     * @param crudAction CRUD action
     */
    protected void queueTriggers(PersistentResource resource, CRUDAction crudAction) {
        queueTriggers(resource, "", crudAction);
    }

    /**
     * Queue triggers for a particular resource and its field.
     *
     * @param resource Resource on which to execute trigger
     * @param fieldName Field name for which to specify trigger
     * @param crudAction CRUD Action
     */
    protected void queueTriggers(PersistentResource resource, String fieldName, CRUDAction crudAction) {
        Consumer<Class> queueTrigger = (cls) -> queuedTriggers.get(cls).add(() -> resource.runTriggers(cls, fieldName));

        switch (crudAction) {
            case CREATE:
                queueTrigger.accept(OnCreatePreSecurity.class);
                queueTrigger.accept(OnCreatePreCommit.class);
                queueTrigger.accept(OnCreatePostCommit.class);
                break;
            case UPDATE:
                queueTrigger.accept(OnUpdatePreSecurity.class);
                queueTrigger.accept(OnUpdatePreCommit.class);
                queueTrigger.accept(OnUpdatePostCommit.class);
                break;
            case DELETE:
                queueTrigger.accept(OnDeletePreSecurity.class);
                queueTrigger.accept(OnDeletePreCommit.class);
                queueTrigger.accept(OnDeletePostCommit.class);
                break;
            case READ:
                queueTrigger.accept(OnReadPreCommit.class);
                queueTrigger.accept(OnReadPostCommit.class);
                break;
            default:
                throw new InternalServerErrorException("Failed to queue trigger of non-actionable CRUD: " + crudAction);
        }
    }

    public void saveOrCreateObjects() {
        dirtyResources.removeAll(newPersistentResources);
        newPersistentResources
                .stream()
                .map(PersistentResource::getObject)
                .forEach(s -> transaction.createObject(s, this));
        dirtyResources.stream().map(PersistentResource::getObject).forEach(obj -> transaction.save(obj, this));
    }
}
