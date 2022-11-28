/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

import static com.yahoo.elide.annotation.LifeCycleHookBinding.ALL_OPERATIONS;

import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.annotation.LifeCycleHookBinding;
import com.yahoo.elide.core.audit.AuditLogger;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.exceptions.BadRequestException;
import com.yahoo.elide.core.exceptions.InvalidAttributeException;
import com.yahoo.elide.core.filter.dialect.ParseException;
import com.yahoo.elide.core.filter.dialect.jsonapi.MultipleFilterDialect;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.lifecycle.CRUDEvent;
import com.yahoo.elide.core.lifecycle.LifecycleHookInvoker;
import com.yahoo.elide.core.request.EntityProjection;
import com.yahoo.elide.core.security.ChangeSpec;
import com.yahoo.elide.core.security.PermissionExecutor;
import com.yahoo.elide.core.security.User;
import com.yahoo.elide.core.security.executors.ActivePermissionExecutor;
import com.yahoo.elide.core.security.executors.MultiplexPermissionExecutor;
import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.jsonapi.JsonApiMapper;
import com.yahoo.elide.jsonapi.models.JsonApiDocument;
import org.apache.commons.collections.MapUtils;

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
public class RequestScope implements com.yahoo.elide.core.security.RequestScope {
    @Getter private final JsonApiDocument jsonApiDocument;
    @Getter private final DataStoreTransaction transaction;
    @Getter private final User user;
    @Getter protected final EntityDictionary dictionary;
    @Getter private final JsonApiMapper mapper;
    @Getter private final AuditLogger auditLogger;
    @Getter private final MultivaluedMap<String, String> queryParams;
    @Getter private final Map<String, Set<String>> sparseFields;
    @Getter private final Map<String, List<String>> requestHeaders;
    @Getter private final PermissionExecutor permissionExecutor;
    @Getter private final ObjectEntityCache objectEntityCache;
    @Getter private final Set<PersistentResource> newPersistentResources;
    @Getter private final LinkedHashSet<PersistentResource> dirtyResources;
    @Getter private final LinkedHashSet<PersistentResource> deletedResources;
    @Getter private final String baseUrlEndPoint;
    @Getter private final String path;
    @Getter private final ElideSettings elideSettings;
    @Getter private final int updateStatusCode;
    @Getter private final MultipleFilterDialect filterDialect;
    @Getter private final String apiVersion;

    //TODO - this ought to be read only and set in the constructor.
    @Getter @Setter private EntityProjection entityProjection;
    @Getter private final UUID requestId;
    private final Map<String, FilterExpression> expressionsByType;

    private final Map<String, Object> metadata;

    private LinkedHashSet<CRUDEvent> eventQueue;

    /* Used to filter across heterogeneous types during the first load */
    private FilterExpression globalFilterExpression;

    /**
     * Create a new RequestScope with specified update status code.
     *
     * @param baseUrlEndPoint base URL with prefix endpoint
     * @param path the URL path
     * @param apiVersion the API version.
     * @param jsonApiDocument the document for this request
     * @param transaction the transaction for this request
     * @param user the user making this request
     * @param queryParams the query parameters
     * @param requestHeaders the requestHeaders
     * @param elideSettings Elide settings object
     */
    public RequestScope(String baseUrlEndPoint,
                        String path,
                        String apiVersion,
                        JsonApiDocument jsonApiDocument,
                        DataStoreTransaction transaction,
                        User user,
                        MultivaluedMap<String, String> queryParams,
                        Map<String, List<String>> requestHeaders,
                        UUID requestId,
                        ElideSettings elideSettings) {
        this.apiVersion = apiVersion;
        this.eventQueue = new LinkedHashSet<>();

        this.path = path;
        this.baseUrlEndPoint = baseUrlEndPoint;
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
        this.metadata = new HashMap<>();
        this.queryParams = queryParams == null ? new MultivaluedHashMap<>() : queryParams;

        this.requestHeaders = MapUtils.isEmpty(requestHeaders)
                ? Collections.emptyMap()
                : requestHeaders;

        this.sparseFields = parseSparseFields(getQueryParams());

        if (!this.queryParams.isEmpty()) {

            /* Extract any query param that starts with 'filter' */
            MultivaluedMap<String, String> filterParams = getFilterParams(this.queryParams);

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
        }

        Function<RequestScope, PermissionExecutor> permissionExecutorGenerator = elideSettings.getPermissionExecutor();
        this.permissionExecutor = new MultiplexPermissionExecutor(
                dictionary.buildPermissionExecutors(this),
                (permissionExecutorGenerator == null)
                        ? new ActivePermissionExecutor(this)
                        : permissionExecutorGenerator.apply(this),
                dictionary
        );
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
        this.baseUrlEndPoint = outerRequestScope.baseUrlEndPoint;
        this.apiVersion = apiVersion;
        this.path = path;
        this.transaction = outerRequestScope.transaction;
        this.user = outerRequestScope.user;
        this.dictionary = outerRequestScope.dictionary;
        this.mapper = outerRequestScope.mapper;
        this.auditLogger = outerRequestScope.auditLogger;
        this.queryParams = new MultivaluedHashMap<>();
        this.requestHeaders = new MultivaluedHashMap<>();
        this.requestHeaders.putAll(outerRequestScope.requestHeaders);
        this.objectEntityCache = outerRequestScope.objectEntityCache;
        this.newPersistentResources = outerRequestScope.newPersistentResources;
        this.permissionExecutor = outerRequestScope.getPermissionExecutor();
        this.dirtyResources = outerRequestScope.dirtyResources;
        this.deletedResources = outerRequestScope.deletedResources;
        this.filterDialect = outerRequestScope.filterDialect;
        this.expressionsByType = outerRequestScope.expressionsByType;
        this.elideSettings = outerRequestScope.elideSettings;
        this.eventQueue = outerRequestScope.eventQueue;
        this.updateStatusCode = outerRequestScope.updateStatusCode;
        this.requestId = outerRequestScope.requestId;
        this.sparseFields = outerRequestScope.sparseFields;
        this.metadata = new HashMap<>(outerRequestScope.metadata);
    }

    public Set<com.yahoo.elide.core.security.PersistentResource> getNewResources() {
        return (Set) newPersistentResources;
    }

    public boolean isNewResource(Object entity) {
        return newPersistentResources.stream().anyMatch(r -> r.getObject() == entity);
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
    public Optional<FilterExpression> getFilterExpressionByType(Type<?> entityClass) {
        return Optional.ofNullable(expressionsByType.get(dictionary.getJsonAliasFor(entityClass)));
    }

    /**
     * Get the global/cross-type filter expression.
     * @param loadClass Entity class
     * @return The global filter expression evaluated at the first load
     */
    public Optional<FilterExpression> getLoadFilterExpression(Type<?> loadClass) {
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
    public Optional<FilterExpression> getExpressionForRelation(Type<?> parentType, String relationName) {
        final Type<?> entityClass = dictionary.getParameterizedType(parentType, relationName);
        if (entityClass == null) {
            throw new InvalidAttributeException(relationName, dictionary.getJsonAliasFor(parentType));
        }

        final String valType = dictionary.getJsonAliasFor(entityClass);
        return getFilterExpressionByType(valType);
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
                .filter(entry -> entry.getKey().startsWith("filter"))
                .forEach(entry -> returnMap.put(entry.getKey(), entry.getValue()));
        return returnMap;
    }

    private void notifySubscribers(
            LifeCycleHookBinding.Operation operation,
            LifeCycleHookBinding.TransactionPhase phase
    ) {
        LifecycleHookInvoker invoker = new LifecycleHookInvoker(dictionary, operation, phase);

        this.eventQueue.stream()
                .filter(event -> event.getEventType().equals(operation))
                .forEach(event -> {
                    invoker.onNext(event);
                });
    }

    /**
     * Run queued pre-security lifecycle triggers.
     */
    public void runQueuedPreSecurityTriggers() {
        notifySubscribers(LifeCycleHookBinding.Operation.CREATE, LifeCycleHookBinding.TransactionPhase.PRESECURITY);
    }

    /**
     * Run queued pre-flush lifecycle triggers.
     */
    public void runQueuedPreFlushTriggers() {
        runQueuedPreFlushTriggers(ALL_OPERATIONS);
    }

    /**
     * Run queued pre-flush lifecycle triggers.
     * @param operations List of operations to run pre-flush triggers for.
     */
    public void runQueuedPreFlushTriggers(LifeCycleHookBinding.Operation[] operations) {
        for (LifeCycleHookBinding.Operation op : operations) {
            notifySubscribers(op, LifeCycleHookBinding.TransactionPhase.PREFLUSH);
        }
    }

    /**
     * Run queued pre-commit lifecycle triggers.
     */
    public void runQueuedPreCommitTriggers() {
        for (LifeCycleHookBinding.Operation op : ALL_OPERATIONS) {
            notifySubscribers(op, LifeCycleHookBinding.TransactionPhase.PRECOMMIT);
        }
    }

    /**
     * Run queued post-commit lifecycle triggers.
     */
    public void runQueuedPostCommitTriggers() {
        for (LifeCycleHookBinding.Operation op : ALL_OPERATIONS) {
            notifySubscribers(op, LifeCycleHookBinding.TransactionPhase.POSTCOMMIT);
        }
    }

    /**
     * Publishes a lifecycle event to all listeners.
     *
     * @param resource Resource on which to execute trigger
     * @param crudAction CRUD action
     */
    protected void publishLifecycleEvent(PersistentResource<?> resource, LifeCycleHookBinding.Operation crudAction) {
        publishLifecycleEvent(new CRUDEvent(crudAction, resource, PersistentResource.CLASS_NO_FIELD, Optional.empty()));
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
        publishLifecycleEvent(new CRUDEvent(crudAction, resource, fieldName, changeSpec));
    }

    protected void publishLifecycleEvent(CRUDEvent event) {
        if (! eventQueue.contains(event)) {
            if (event.getEventType().equals(LifeCycleHookBinding.Operation.DELETE)
                    || event.getEventType().equals(LifeCycleHookBinding.Operation.UPDATE)) {

                LifecycleHookInvoker invoker = new LifecycleHookInvoker(dictionary,
                        event.getEventType(),
                        LifeCycleHookBinding.TransactionPhase.PRESECURITY);

                invoker.onNext(event);
            }

            eventQueue.add(event);
        }
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

    public Object getObjectById(Type<?> type, String id) {
        Type<?> boundType = dictionary.lookupBoundClass(type);

        Object result = objectEntityCache.get(boundType.getName(), id);

        // Check inheritance too
        Iterator<Type<?>> it = dictionary.getSubclassingEntities(boundType).iterator();
        while (result == null && it.hasNext()) {
            String newType = getInheritanceKey(it.next().getName(), boundType.getName());
            result = objectEntityCache.get(newType, id);
        }

        return result;
    }

    public void setUUIDForObject(Type<?> type, String id, Object object) {
        Type<?> boundType = dictionary.lookupBoundClass(type);

        objectEntityCache.put(boundType.getName(), id, object);

        // Insert for all inherited entities as well
        dictionary.getSuperClassEntities(type).stream()
                .map(i -> getInheritanceKey(boundType.getName(), i.getName()))
                .forEach((newType) -> objectEntityCache.put(newType, id, object));
    }

    private String getInheritanceKey(String subClass, String superClass) {
        return subClass + "!" + superClass;
    }

    @Override
    public String getRequestHeaderByName(String headerName) {
        if (this.requestHeaders.get(headerName) == null) {
            return null;
        }
        return this.requestHeaders.get(headerName).get(0);
    }

    @Override
    public void setMetadataField(String property, Object value) {
        metadata.put(property, value);
    }

    @Override
    public Optional<Object> getMetadataField(String property) {
        return Optional.ofNullable(metadata.getOrDefault(property, null));
    }

    @Override
    public Set<String> getMetadataFields() {
        return metadata.keySet();
    }
}
