/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.annotation.OnCreatePostCommit;
import com.yahoo.elide.annotation.OnCreatePreCommit;
import com.yahoo.elide.annotation.OnCreatePreSecurity;
import com.yahoo.elide.annotation.OnDeletePostCommit;
import com.yahoo.elide.annotation.OnDeletePreCommit;
import com.yahoo.elide.annotation.OnDeletePreSecurity;
import com.yahoo.elide.annotation.OnReadPostCommit;
import com.yahoo.elide.annotation.OnReadPreCommit;
import com.yahoo.elide.annotation.OnReadPreSecurity;
import com.yahoo.elide.annotation.OnUpdatePostCommit;
import com.yahoo.elide.annotation.OnUpdatePreCommit;
import com.yahoo.elide.annotation.OnUpdatePreSecurity;
import com.yahoo.elide.audit.AuditLogger;
import com.yahoo.elide.core.exceptions.InvalidAttributeException;
import com.yahoo.elide.core.exceptions.InvalidOperationException;
import com.yahoo.elide.core.exceptions.InvalidPredicateException;
import com.yahoo.elide.core.filter.dialect.MultipleFilterDialect;
import com.yahoo.elide.core.filter.dialect.ParseException;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.pagination.Pagination;
import com.yahoo.elide.core.sort.Sorting;
import com.yahoo.elide.jsonapi.JsonApiMapper;
import com.yahoo.elide.jsonapi.models.JsonApiDocument;
import com.yahoo.elide.security.ChangeSpec;
import com.yahoo.elide.security.PermissionExecutor;
import com.yahoo.elide.security.User;
import com.yahoo.elide.security.executors.ActivePermissionExecutor;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.ReplaySubject;
import lombok.Getter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

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
    @Getter private final PermissionExecutor permissionExecutor;
    @Getter private final ObjectEntityCache objectEntityCache;
    @Getter private final Set<PersistentResource> newPersistentResources;
    @Getter private final LinkedHashSet<PersistentResource> dirtyResources;
    @Getter private final LinkedHashSet<PersistentResource> deletedResources;
    @Getter private final String path;
    @Getter private final ElideSettings elideSettings;
    @Getter private final boolean useFilterExpressions;
    @Getter private final int updateStatusCode;

    @Getter private final MultipleFilterDialect filterDialect;
    private final Map<String, FilterExpression> expressionsByType;

    private PublishSubject<CRUDEvent> lifecycleEvents;
    private Observable<CRUDEvent> distinctLifecycleEvents;
    private ReplaySubject<CRUDEvent> queuedLifecycleEvents;

    /* Used to filter across heterogeneous types during the first load */
    private FilterExpression globalFilterExpression;

    /**
     * Create a new RequestScope with specified update status code.
     *
     * @param path the URL path
     * @param jsonApiDocument the document for this request
     * @param transaction the transaction for this request
     * @param user the user making this request
     * @param queryParams the query parameters
     * @param elideSettings Elide settings object
     */
    public RequestScope(String path,
                        JsonApiDocument jsonApiDocument,
                        DataStoreTransaction transaction,
                        User user,
                        MultivaluedMap<String, String> queryParams,
                        ElideSettings elideSettings) {
        this.lifecycleEvents = PublishSubject.create();
        this.distinctLifecycleEvents = lifecycleEvents.distinct();
        this.queuedLifecycleEvents = ReplaySubject.create();
        this.distinctLifecycleEvents.subscribe(queuedLifecycleEvents);

        this.path = path;
        this.jsonApiDocument = jsonApiDocument;
        this.transaction = transaction;
        this.user = user;
        this.dictionary = elideSettings.getDictionary();
        this.mapper = elideSettings.getMapper();
        this.auditLogger = elideSettings.getAuditLogger();
        this.filterDialect = new MultipleFilterDialect(elideSettings.getJoinFilterDialects(),
                elideSettings.getSubqueryFilterDialects());
        this.elideSettings = elideSettings;
        this.useFilterExpressions = elideSettings.isUseFilterExpressions();
        this.updateStatusCode = elideSettings.getUpdateStatusCode();

        this.globalFilterExpression = null;
        this.expressionsByType = new HashMap<>();
        this.objectEntityCache = new ObjectEntityCache();
        this.newPersistentResources = new LinkedHashSet<>();
        this.dirtyResources = new LinkedHashSet<>();
        this.deletedResources = new LinkedHashSet<>();

        Function<RequestScope, PermissionExecutor> permissionExecutorGenerator = elideSettings.getPermissionExecutor();
        this.permissionExecutor = (permissionExecutorGenerator == null)
                ? new ActivePermissionExecutor(this)
                : permissionExecutorGenerator.apply(this);

        this.queryParams = (queryParams == null || queryParams.size() == 0)
                ? Optional.empty()
                : Optional.of(queryParams);

        registerPreSecurityObservers();

        if (this.queryParams.isPresent()) {

            /* Extract any query param that starts with 'filter' */
            MultivaluedMap<String, String> filterParams = getFilterParams(queryParams);

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
     * @param path the URL path
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
        this.permissionExecutor = outerRequestScope.getPermissionExecutor();
        this.dirtyResources = outerRequestScope.dirtyResources;
        this.deletedResources = outerRequestScope.deletedResources;
        this.filterDialect = outerRequestScope.filterDialect;
        this.expressionsByType = outerRequestScope.expressionsByType;
        this.elideSettings = outerRequestScope.elideSettings;
        this.useFilterExpressions = outerRequestScope.useFilterExpressions;
        this.updateStatusCode = outerRequestScope.updateStatusCode;
        this.lifecycleEvents = outerRequestScope.lifecycleEvents;
        this.distinctLifecycleEvents = outerRequestScope.distinctLifecycleEvents;
        this.queuedLifecycleEvents = outerRequestScope.queuedLifecycleEvents;
    }

    @Override
    public Set<com.yahoo.elide.security.PersistentResource> getNewResources() {
        return (Set) newPersistentResources;
    }

    public boolean isNewResource(Object entity) {
        return newPersistentResources.stream().filter(r -> r.getObject() == entity).findAny().isPresent();
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
     * @param loadClass Entity class
     * @return The global filter expression evaluated at the first load
     */
    public Optional<FilterExpression> getLoadFilterExpression(Class<?> loadClass) {
        Optional<FilterExpression> filterExpression;
        if (globalFilterExpression == null) {
            String typeName = dictionary.getJsonAliasFor(loadClass);
            filterExpression =  getFilterExpressionByType(typeName);
        } else {
            filterExpression = Optional.of(globalFilterExpression);
        }
        return filterExpression;
    }

    /**
     * Get the filter expression for a particular relationship
     * @param parent The object which has the relationship
     * @param relationName The relationship name
     * @return A type specific filter expression for the given relationship
     */
    public Optional<FilterExpression> getExpressionForRelation(PersistentResource parent, String relationName) {
        final Class<?> entityClass = dictionary.getParameterizedType(parent.getObject(), relationName);
        if (entityClass == null) {
            throw new InvalidAttributeException(relationName, parent.getType());
        }
        if (dictionary.isMappedInterface(entityClass) && interfaceHasFilterExpression(entityClass)) {
            throw new InvalidOperationException(
                    "Cannot apply filters to polymorphic relations mapped with MappedInterface");
        }
        final String valType = dictionary.getJsonAliasFor(entityClass);
        return getFilterExpressionByType(valType);
    }

    /**
     * Checks to see if any filters are meant to to applied to a polymorphic Any/ManyToAny relationship.
     * @param entityInterface a @MappedInterface
     * @return whether or not there are any typed filter expressions meant for this polymorphic interface
     */
    private boolean interfaceHasFilterExpression(Class<?> entityInterface) {
        for (String filterType : expressionsByType.keySet()) {
            Class<?> polyMorphicClass = dictionary.getEntityClass(filterType);
            if (entityInterface.isAssignableFrom(polyMorphicClass)) {
                return true;
            }
        }
        return false;
    }


    /**
     * Extracts any query params that start with 'filter'.
     * @param queryParams request query params
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
     * Run queued on triggers (i.e. @OnCreatePreSecurity, @OnUpdatePreSecurity, etc.).
     */
    public void runQueuedPreSecurityTriggers() {
        this.queuedLifecycleEvents
                .filter(CRUDEvent::isCreateEvent)
                .subscribeWith(new LifecycleHookInvoker(dictionary, OnCreatePreSecurity.class, false))
                .throwOnError();
    }

    /**
     * Run queued pre triggers (i.e. @OnCreatePreCommit, @OnUpdatePreCommit, etc.).
     */
    public void runQueuedPreCommitTriggers() {
        this.queuedLifecycleEvents
                .filter(CRUDEvent::isCreateEvent)
                .subscribeWith(new LifecycleHookInvoker(dictionary, OnCreatePreCommit.class, false))
                .throwOnError();

        this.queuedLifecycleEvents
                .filter(CRUDEvent::isUpdateEvent)
                .subscribeWith(new LifecycleHookInvoker(dictionary, OnUpdatePreCommit.class, false))
                .throwOnError();

        this.queuedLifecycleEvents
                .filter(CRUDEvent::isDeleteEvent)
                .subscribeWith(new LifecycleHookInvoker(dictionary, OnDeletePreCommit.class, false))
                .throwOnError();

        this.queuedLifecycleEvents
                .filter(CRUDEvent::isReadEvent)
                .subscribeWith(new LifecycleHookInvoker(dictionary, OnReadPreCommit.class, false))
                .throwOnError();
    }

    /**
     * Run queued post triggers (i.e. @OnCreatePostCommit, @OnUpdatePostCommit, etc.).
     */
    public void runQueuedPostCommitTriggers() {
        this.queuedLifecycleEvents
                .filter(CRUDEvent::isCreateEvent)
                .subscribeWith(new LifecycleHookInvoker(dictionary, OnCreatePostCommit.class, false))
                .throwOnError();

        this.queuedLifecycleEvents
                .filter(CRUDEvent::isUpdateEvent)
                .subscribeWith(new LifecycleHookInvoker(dictionary, OnUpdatePostCommit.class, false))
                .throwOnError();

        this.queuedLifecycleEvents
                .filter(CRUDEvent::isDeleteEvent)
                .subscribeWith(new LifecycleHookInvoker(dictionary, OnDeletePostCommit.class, false))
                .throwOnError();

        this.queuedLifecycleEvents
                .filter(CRUDEvent::isReadEvent)
                .subscribeWith(new LifecycleHookInvoker(dictionary, OnReadPostCommit.class, false))
                .throwOnError();
    }

    /**
     * Publishes a lifecycle event to all listeners.
     *
     * @param resource Resource on which to execute trigger
     * @param crudAction CRUD action
     */
    protected void publishLifecycleEvent(PersistentResource<?> resource, CRUDEvent.CRUDAction crudAction) {
        lifecycleEvents.onNext(
                    new CRUDEvent(crudAction, resource, PersistentResource.CLASS_NO_FIELD, Optional.empty())
        );
    }

    /**
     * Publishes a lifecycle event to all listeners.
     *
     * @param resource Resource on which to execute trigger
     * @param fieldName Field name for which to specify trigger
     * @param crudAction CRUD Action
     * @param changeSpec Optional ChangeSpec to pass to the lifecycle hook
     */
    protected void publishLifecycleEvent(PersistentResource<?> resource,
                                         String fieldName,
                                         CRUDEvent.CRUDAction crudAction,
                                         Optional<ChangeSpec> changeSpec) {
        lifecycleEvents.onNext(
                    new CRUDEvent(crudAction, resource, fieldName, changeSpec)
        );
    }

    public void saveOrCreateObjects() {
        dirtyResources.removeAll(newPersistentResources);
        // Delete has already been called on these objects
        dirtyResources.removeAll(deletedResources);
        newPersistentResources
                .stream()
                .map(PersistentResource::getObject)
                .forEach(s -> transaction.createObject(s, this));
        dirtyResources.stream().map(PersistentResource::getObject).forEach(obj -> transaction.save(obj, this));
    }

    public String getUUIDFor(Object o) {
        return objectEntityCache.getUUID(o);
    }

    public Object getObjectById(String type, String id) {
        Object result = objectEntityCache.get(type, id);

        // Check inheritance too
        Iterator<String> it = dictionary.getSubclassingEntityNames(type).iterator();
        while (result == null && it.hasNext()) {
            String newType = getInheritanceKey(it.next(), type);
            result = objectEntityCache.get(newType, id);
        }

        return result;
    }

    public void setUUIDForObject(String type, String id, Object object) {
        objectEntityCache.put(type, id, object);

        // Insert for all inherited entities as well
        dictionary.getSuperClassEntityNames(type).stream()
                .map(i -> getInheritanceKey(type, i))
                .forEach((newType) -> objectEntityCache.put(newType, id, object));
    }

    private String getInheritanceKey(String subClass, String superClass) {
        return subClass + "!" + superClass;
    }

    private void registerPreSecurityObservers() {

        this.distinctLifecycleEvents
                .filter(CRUDEvent::isReadEvent)
                .subscribeWith(new LifecycleHookInvoker(dictionary, OnReadPreSecurity.class, true));

        this.distinctLifecycleEvents
                .filter(CRUDEvent::isUpdateEvent)
                .subscribeWith(new LifecycleHookInvoker(dictionary, OnUpdatePreSecurity.class, true));

        this.distinctLifecycleEvents
                .filter(CRUDEvent::isDeleteEvent)
                .subscribeWith(new LifecycleHookInvoker(dictionary, OnDeletePreSecurity.class, true));
    }
}
