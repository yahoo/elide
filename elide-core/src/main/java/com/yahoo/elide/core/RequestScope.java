/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.annotation.LifeCycleHookBinding;
import com.yahoo.elide.audit.AuditLogger;
import com.yahoo.elide.core.exceptions.BadRequestException;
import com.yahoo.elide.core.exceptions.InvalidAttributeException;
import com.yahoo.elide.core.exceptions.InvalidOperationException;
import com.yahoo.elide.core.filter.dialect.MultipleFilterDialect;
import com.yahoo.elide.core.filter.dialect.ParseException;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.jsonapi.JsonApiMapper;
import com.yahoo.elide.jsonapi.models.JsonApiDocument;
import com.yahoo.elide.request.EntityProjection;
import com.yahoo.elide.security.ChangeSpec;
import com.yahoo.elide.security.PermissionExecutor;
import com.yahoo.elide.security.User;
import com.yahoo.elide.security.executors.ActivePermissionExecutor;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.ReplaySubject;
import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
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
    @Getter protected final EntityDictionary dictionary;
    @Getter private final JsonApiMapper mapper;
    @Getter private final AuditLogger auditLogger;
    @Getter private final Optional<MultivaluedMap<String, String>> queryParams;
    @Getter private final Map<String, Set<String>> sparseFields;
    @Getter private final PermissionExecutor permissionExecutor;
    @Getter private final ObjectEntityCache objectEntityCache;
    @Getter private final Set<PersistentResource> newPersistentResources;
    @Getter private final LinkedHashSet<PersistentResource> dirtyResources;
    @Getter private final LinkedHashSet<PersistentResource> deletedResources;
    @Getter private final String path;
    @Getter private final ElideSettings elideSettings;
    @Getter private final int updateStatusCode;
    @Getter private final MultipleFilterDialect filterDialect;
    @Getter private final String apiVersion;
    @Getter @Setter private Map<String, String> headers;

    //TODO - this ought to be read only and set in the constructor.
    @Getter @Setter private EntityProjection entityProjection;
    @Getter private final UUID requestId;
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
     * @param apiVersion the API version.
     * @param jsonApiDocument the document for this request
     * @param transaction the transaction for this request
     * @param user the user making this request
     * @param queryParams the query parameters
     * @param elideSettings Elide settings object
     */
    public RequestScope(String path,
                        String apiVersion,
                        JsonApiDocument jsonApiDocument,
                        DataStoreTransaction transaction,
                        User user,
                        MultivaluedMap<String, String> queryParams,
                        UUID requestId,
                        ElideSettings elideSettings) {
        this.apiVersion = apiVersion;
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
        this.updateStatusCode = elideSettings.getUpdateStatusCode();

        this.globalFilterExpression = null;
        this.expressionsByType = new HashMap<>();
        this.objectEntityCache = new ObjectEntityCache();
        this.newPersistentResources = new LinkedHashSet<>();
        this.dirtyResources = new LinkedHashSet<>();
        this.deletedResources = new LinkedHashSet<>();
        this.requestId = requestId;
        this.headers = new HashMap<>();

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
                    globalFilterExpression = filterDialect.parseGlobalExpression(path, filterParams, apiVersion);
                } catch (ParseException e) {
                    errorMessage = e.getMessage();
                }

                /* Next check to see if there is are type specific filters */
                try {
                    expressionsByType.putAll(filterDialect.parseTypedExpression(path, filterParams, apiVersion));
                } catch (ParseException e) {

                    /* If neither dialect parsed, report the last error found */
                    if (globalFilterExpression == null) {

                        if (errorMessage.isEmpty()) {
                            errorMessage = e.getMessage();
                        } else if (! errorMessage.equals(e.getMessage())) {

                            /* Combine the two different messages together */
                            errorMessage = errorMessage + "\n" + e.getMessage();
                        }

                        throw new BadRequestException(errorMessage, e);
                    }
                }
            }

            this.sparseFields = parseSparseFields(queryParams);
        } else {
            this.sparseFields = Collections.emptyMap();
        }
    }

    /**
     * Special copy constructor for use by PatchRequestScope.
     *
     * @param path the URL path
     * @param apiVersion the API version
     * @param jsonApiDocument   the json api document
     * @param outerRequestScope the outer request scope
     */
    protected RequestScope(String path, String apiVersion,
                           JsonApiDocument jsonApiDocument, RequestScope outerRequestScope) {
        this.jsonApiDocument = jsonApiDocument;
        this.apiVersion = apiVersion;
        this.path = path;
        this.transaction = outerRequestScope.transaction;
        this.user = outerRequestScope.user;
        this.dictionary = outerRequestScope.dictionary;
        this.mapper = outerRequestScope.mapper;
        this.auditLogger = outerRequestScope.auditLogger;
        this.queryParams = Optional.empty();
        this.sparseFields = Collections.emptyMap();
        this.objectEntityCache = outerRequestScope.objectEntityCache;
        this.newPersistentResources = outerRequestScope.newPersistentResources;
        this.permissionExecutor = outerRequestScope.getPermissionExecutor();
        this.dirtyResources = outerRequestScope.dirtyResources;
        this.deletedResources = outerRequestScope.deletedResources;
        this.filterDialect = outerRequestScope.filterDialect;
        this.expressionsByType = outerRequestScope.expressionsByType;
        this.elideSettings = outerRequestScope.elideSettings;
        this.lifecycleEvents = outerRequestScope.lifecycleEvents;
        this.distinctLifecycleEvents = outerRequestScope.distinctLifecycleEvents;
        this.updateStatusCode = outerRequestScope.updateStatusCode;
        this.queuedLifecycleEvents = outerRequestScope.queuedLifecycleEvents;
        this.requestId = outerRequestScope.requestId;
        this.headers = outerRequestScope.headers;
    }

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
    public static Map<String, Set<String>> parseSparseFields(MultivaluedMap<String, String> queryParams) {
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
     * Get filter expression for a specific collection type.
     * @param entityClass The class to lookup
     * @return The filter expression for the given type
     */
    public Optional<FilterExpression> getFilterExpressionByType(Class<?> entityClass) {
        return Optional.ofNullable(expressionsByType.get(dictionary.getJsonAliasFor(entityClass)));
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
     * Get the filter expression for a particular relationship.
     * @param parentType The parent type which has the relationship
     * @param relationName The relationship name
     * @return A type specific filter expression for the given relationship
     */
    public Optional<FilterExpression> getExpressionForRelation(Class<?> parentType, String relationName) {
        final Class<?> entityClass = dictionary.getParameterizedType(parentType, relationName);
        if (entityClass == null) {
            throw new InvalidAttributeException(relationName, dictionary.getJsonAliasFor(parentType));
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
            String version = EntityDictionary.getModelVersion(entityInterface);
            Class<?> polyMorphicClass = dictionary.getEntityClass(filterType, version);
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
     * Run queued pre-security lifecycle triggers.
     */
    public void runQueuedPreSecurityTriggers() {
        this.queuedLifecycleEvents
                .filter(CRUDEvent::isCreateEvent)
                .subscribeWith(new LifecycleHookInvoker(dictionary,
                        LifeCycleHookBinding.Operation.CREATE,
                        LifeCycleHookBinding.TransactionPhase.PRESECURITY, false))
                .throwOnError();
    }

    /**
     * Run queued pre-commit lifecycle triggers.
     */
    public void runQueuedPreCommitTriggers() {
        this.queuedLifecycleEvents
                .filter(CRUDEvent::isCreateEvent)
                .subscribeWith(new LifecycleHookInvoker(dictionary,
                        LifeCycleHookBinding.Operation.CREATE,
                        LifeCycleHookBinding.TransactionPhase.PRECOMMIT, false))
                .throwOnError();

        this.queuedLifecycleEvents
                .filter(CRUDEvent::isUpdateEvent)
                .subscribeWith(new LifecycleHookInvoker(dictionary,
                        LifeCycleHookBinding.Operation.UPDATE,
                        LifeCycleHookBinding.TransactionPhase.PRECOMMIT, false))
                .throwOnError();

        this.queuedLifecycleEvents
                .filter(CRUDEvent::isDeleteEvent)
                .subscribeWith(new LifecycleHookInvoker(dictionary,
                        LifeCycleHookBinding.Operation.DELETE,
                        LifeCycleHookBinding.TransactionPhase.PRECOMMIT, false))
                .throwOnError();

        this.queuedLifecycleEvents
                .filter(CRUDEvent::isReadEvent)
                .subscribeWith(new LifecycleHookInvoker(dictionary,
                        LifeCycleHookBinding.Operation.READ,
                        LifeCycleHookBinding.TransactionPhase.PRECOMMIT, false))
                .throwOnError();
    }

    /**
     * Run queued post-commit lifecycle triggers.
     */
    public void runQueuedPostCommitTriggers() {
        this.queuedLifecycleEvents
                .filter(CRUDEvent::isCreateEvent)
                .subscribeWith(new LifecycleHookInvoker(dictionary,
                        LifeCycleHookBinding.Operation.CREATE,
                        LifeCycleHookBinding.TransactionPhase.POSTCOMMIT, false))
                .throwOnError();

        this.queuedLifecycleEvents
                .filter(CRUDEvent::isUpdateEvent)
                .subscribeWith(new LifecycleHookInvoker(dictionary,
                        LifeCycleHookBinding.Operation.UPDATE,
                        LifeCycleHookBinding.TransactionPhase.POSTCOMMIT, false))
                .throwOnError();

        this.queuedLifecycleEvents
                .filter(CRUDEvent::isDeleteEvent)
                .subscribeWith(new LifecycleHookInvoker(dictionary,
                        LifeCycleHookBinding.Operation.DELETE,
                        LifeCycleHookBinding.TransactionPhase.POSTCOMMIT, false))
                .throwOnError();

        this.queuedLifecycleEvents
                .filter(CRUDEvent::isReadEvent)
                .subscribeWith(new LifecycleHookInvoker(dictionary,
                        LifeCycleHookBinding.Operation.READ,
                        LifeCycleHookBinding.TransactionPhase.POSTCOMMIT, false))
                .throwOnError();
    }

    /**
     * Publishes a lifecycle event to all listeners.
     *
     * @param resource Resource on which to execute trigger
     * @param crudAction CRUD action
     */
    protected void publishLifecycleEvent(PersistentResource<?> resource, LifeCycleHookBinding.Operation crudAction) {
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
                                         LifeCycleHookBinding.Operation crudAction,
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

    public Object getObjectById(Class<?> type, String id) {
        Class<?> boundType = dictionary.lookupBoundClass(type);

        Object result = objectEntityCache.get(boundType.getName(), id);

        // Check inheritance too
        Iterator<Class<?>> it = dictionary.getSubclassingEntities(boundType).iterator();
        while (result == null && it.hasNext()) {
            String newType = getInheritanceKey(it.next().getName(), boundType.getName());
            result = objectEntityCache.get(newType, id);
        }

        return result;
    }

    public void setUUIDForObject(Class<?> type, String id, Object object) {
        Class<?> boundType = dictionary.lookupBoundClass(type);

        objectEntityCache.put(boundType.getName(), id, object);

        // Insert for all inherited entities as well
        dictionary.getSuperClassEntities(type).stream()
                .map(i -> getInheritanceKey(boundType.getName(), i.getName()))
                .forEach((newType) -> objectEntityCache.put(newType, id, object));
    }

    private String getInheritanceKey(String subClass, String superClass) {
        return subClass + "!" + superClass;
    }

    private void registerPreSecurityObservers() {

        this.distinctLifecycleEvents
                .filter(CRUDEvent::isReadEvent)
                .subscribeWith(new LifecycleHookInvoker(dictionary,
                        LifeCycleHookBinding.Operation.READ,
                        LifeCycleHookBinding.TransactionPhase.PRESECURITY, true));

        this.distinctLifecycleEvents
                .filter(CRUDEvent::isUpdateEvent)
                .subscribeWith(new LifecycleHookInvoker(dictionary,
                        LifeCycleHookBinding.Operation.UPDATE,
                        LifeCycleHookBinding.TransactionPhase.PRESECURITY, true));

        this.distinctLifecycleEvents
                .filter(CRUDEvent::isDeleteEvent)
                .subscribeWith(new LifecycleHookInvoker(dictionary,
                        LifeCycleHookBinding.Operation.DELETE,
                        LifeCycleHookBinding.TransactionPhase.PRESECURITY, true));
    }
}
