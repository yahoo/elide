/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core;

import static com.paiondata.elide.annotation.LifeCycleHookBinding.ALL_OPERATIONS;

import com.paiondata.elide.ElideSettings;
import com.paiondata.elide.annotation.LifeCycleHookBinding;
import com.paiondata.elide.core.audit.AuditLogger;
import com.paiondata.elide.core.datastore.DataStoreTransaction;
import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.exceptions.InvalidAttributeException;
import com.paiondata.elide.core.filter.expression.FilterExpression;
import com.paiondata.elide.core.lifecycle.CRUDEvent;
import com.paiondata.elide.core.lifecycle.LifecycleHookInvoker;
import com.paiondata.elide.core.request.EntityProjection;
import com.paiondata.elide.core.request.route.Route;
import com.paiondata.elide.core.security.ChangeSpec;
import com.paiondata.elide.core.security.PermissionExecutor;
import com.paiondata.elide.core.security.User;
import com.paiondata.elide.core.security.executors.ActivePermissionExecutor;
import com.paiondata.elide.core.security.executors.MultiplexPermissionExecutor;
import com.paiondata.elide.core.type.Type;

import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

/**
 * Request scope object for relaying request-related data to various subsystems.
 */
public class RequestScope implements com.paiondata.elide.core.security.RequestScope {
    protected Route route;
    @Getter private final DataStoreTransaction transaction;
    @Getter private final User user;
    @Getter protected final EntityDictionary dictionary;
    @Getter private final AuditLogger auditLogger;
    @Getter private final PermissionExecutor permissionExecutor;
    @Getter private final ObjectEntityCache objectEntityCache;
    @Getter private final Set<PersistentResource> newPersistentResources;
    @Getter private final LinkedHashSet<PersistentResource> dirtyResources;
    @Getter private final LinkedHashSet<PersistentResource> deletedResources;
    private final ElideSettings elideSettings;
    @Getter private final Map<String, Set<String>> sparseFields;
    //TODO - this ought to be read only and set in the constructor.
    @Getter @Setter private EntityProjection entityProjection;
    protected Function<RequestScope, EntityProjection> entityProjectionResolver;
    @Getter private final UUID requestId;

    protected Map<String, FilterExpression> expressionsByType;

    private final Map<String, Object> metadata;

    private LinkedHashSet<CRUDEvent> eventQueue;

    /* Used to filter across heterogeneous types during the first load */
    protected FilterExpression globalFilterExpression;

    /**
     * Create a new RequestScope.
     *
     * @param route         the route
     * @param transaction   current transaction
     * @param user          request user
     * @param requestId     request ID
     * @param entityProjection entity projection
     */
    public RequestScope(Route route,
                        DataStoreTransaction transaction,
                        User user,
                        UUID requestId,
                        ElideSettings elideSettings,
                        Function<RequestScope, EntityProjection> entityProjection
                        ) {
        this.route = route;
        this.eventQueue = new LinkedHashSet<>();

        this.transaction = transaction;
        this.user = user;
        this.dictionary = elideSettings.getEntityDictionary();
        this.auditLogger = elideSettings.getAuditLogger();
        this.elideSettings = elideSettings;

        this.globalFilterExpression = null;
        this.expressionsByType = new LinkedHashMap<>();
        this.objectEntityCache = new ObjectEntityCache();
        this.newPersistentResources = new LinkedHashSet<>();
        this.dirtyResources = new LinkedHashSet<>();
        this.deletedResources = new LinkedHashSet<>();
        this.requestId = requestId;
        this.metadata = new LinkedHashMap<>();

        this.sparseFields = parseSparseFields(getRoute().getParameters());

        this.entityProjectionResolver = entityProjection;
        if (this.entityProjectionResolver != null) {
            this.entityProjection = this.entityProjectionResolver.apply(this);
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

    public RequestScope(RequestScope copy) {
        this.route = copy.route;
        this.transaction = copy.transaction;
        this.user = copy.user;
        this.dictionary = copy.dictionary;
        this.auditLogger = copy.auditLogger;
        this.objectEntityCache = copy.objectEntityCache;
        this.newPersistentResources = copy.newPersistentResources;
        this.dirtyResources = copy.dirtyResources;
        this.deletedResources = copy.deletedResources;
        this.elideSettings = copy.elideSettings;
        this.sparseFields = copy.sparseFields;
        this.requestId = copy.requestId;
        this.expressionsByType = copy.expressionsByType;
        this.metadata = copy.metadata;
        this.eventQueue = copy.eventQueue;
        this.globalFilterExpression = copy.globalFilterExpression;

        this.permissionExecutor = copy.permissionExecutor;

        this.entityProjectionResolver = copy.entityProjectionResolver;
        if (this.entityProjectionResolver != null) {
            this.entityProjection = this.entityProjectionResolver.apply(this);
        }
    }

    public Set<com.paiondata.elide.core.security.PersistentResource> getNewResources() {
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
    public static Map<String, Set<String>> parseSparseFields(Map<String, List<String>> queryParams) {
        Map<String, Set<String>> result = new LinkedHashMap<>();

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
    private static Map<String, List<String>> getFilterParams(Map<String, List<String>> queryParams) {
        Map<String, List<String>> returnMap = new LinkedHashMap<>();

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
                .forEach(newType -> objectEntityCache.put(newType, id, object));
    }

    private String getInheritanceKey(String subClass, String superClass) {
        return subClass + "!" + superClass;
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

    @Override
    public Route getRoute() {
        return this.route;
    }

    @Override
    public ElideSettings getElideSettings() {
        return this.elideSettings;
    }

    /**
     * Returns a mutable {@link RequestScopeBuilder} for building {@link RequestScope}.
     *
     * @return the builder
     */
    public static RequestScopeBuilder builder() {
        return new RequestScopeBuilder();
    }

    /**
     * A mutable builder for building {@link RequestScope}.
     */
    public static class RequestScopeBuilder {
        protected Route route;
        protected DataStoreTransaction dataStoreTransaction;
        protected User user;
        protected UUID requestId;
        protected ElideSettings elideSettings;
        protected Function<RequestScope, EntityProjection> entityProjection;

        protected void applyDefaults() {
            if (this.requestId == null) {
                this.requestId = UUID.randomUUID();
            }
        }

        public RequestScope build() {
            applyDefaults();
            return new RequestScope(this.route, this.dataStoreTransaction, this.user, this.requestId,
                    this.elideSettings, this.entityProjection);
        }

        public RequestScopeBuilder route(Route route) {
            this.route = route;
            return this;
        }

        public RequestScopeBuilder dataStoreTransaction(DataStoreTransaction transaction) {
            this.dataStoreTransaction = transaction;
            return this;
        }

        public RequestScopeBuilder user(User user) {
            this.user = user;
            return this;
        }

        public RequestScopeBuilder requestId(UUID requestId) {
            this.requestId = requestId;
            return this;
        }

        public RequestScopeBuilder elideSettings(ElideSettings elideSettings) {
            this.elideSettings = elideSettings;
            return this;
        }

        public RequestScopeBuilder entityProjection(Function<RequestScope, EntityProjection> entityProjection) {
            this.entityProjection = entityProjection;
            return this;
        }

        public RequestScopeBuilder entityProjection(EntityProjection entityProjection) {
            this.entityProjection = requestScope -> entityProjection;
            return this;
        }
    }
}
