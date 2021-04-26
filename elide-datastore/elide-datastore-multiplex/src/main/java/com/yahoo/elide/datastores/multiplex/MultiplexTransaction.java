/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.multiplex;

import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.datastore.DataStore;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.dictionary.RelationshipType;
import com.yahoo.elide.core.exceptions.InvalidCollectionException;
import com.yahoo.elide.core.filter.Operator;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.expression.PredicateExtractionVisitor;
import com.yahoo.elide.core.filter.predicates.FilterPredicate;
import com.yahoo.elide.core.request.Attribute;
import com.yahoo.elide.core.request.EntityProjection;
import com.yahoo.elide.core.request.Relationship;
import com.yahoo.elide.core.type.ClassType;
import com.yahoo.elide.core.type.Type;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.Set;

/**
 * Multiplex transaction handler.  Process each sub-database transactions within a single transaction.
 * If any commit fails in process, reverse any commits already completed.
 */
public abstract class MultiplexTransaction implements DataStoreTransaction {
    protected LinkedHashMap<DataStore, DataStoreTransaction> transactions;
    protected final MultiplexManager multiplexManager;

    /**
     * Multiplex transaction handler.
     * @param multiplexManager associated manager
     */
    public MultiplexTransaction(MultiplexManager multiplexManager) {
        this.multiplexManager = multiplexManager;
        this.transactions = new LinkedHashMap<>(multiplexManager.dataStores.size());
    }

    protected abstract DataStoreTransaction beginTransaction(DataStore dataStore);

    @Override
    public <T> void createObject(T entity, RequestScope scope) {
        getTransaction(EntityDictionary.getType(entity)).createObject(entity, scope);
    }

    @Override
    public <T> T loadObject(EntityProjection projection,
                             Serializable id,
                             RequestScope scope) {
        return getTransaction(projection.getType()).loadObject(projection, id, scope);
    }

    @Override
    public <T> Iterable<T> loadObjects(
            EntityProjection projection,
            RequestScope scope) {
        return getTransaction(projection.getType()).loadObjects(projection, scope);
    }

    @Override
    public void flush(RequestScope requestScope) {
        transactions.values().stream()
                .filter(dataStoreTransaction -> dataStoreTransaction != null)
                .forEach(dataStoreTransaction -> dataStoreTransaction.flush(requestScope));
    }

    @Override
    public void preCommit(RequestScope scope) {
        transactions.values().stream()
                .filter(dataStoreTransaction -> dataStoreTransaction != null)
                .forEach(tx -> tx.preCommit(scope));
    }

    @Override
    public void commit(RequestScope scope) {
        // flush all before commit
        flush(scope);
        transactions.values().stream()
               .filter(dataStoreTransaction -> dataStoreTransaction != null)
               .forEach(dataStoreTransaction -> dataStoreTransaction.commit(scope));
    }

    @Override
    public void close() throws IOException {

        IOException cause = null;
        for (DataStoreTransaction transaction : transactions.values()) {
            try {
                transaction.close();
            } catch (IOException | Error | RuntimeException e) {
                if (cause != null) {
                    cause.addSuppressed(e);
                } else if (e instanceof IOException) {
                    cause = (IOException) e;
                } else {
                    cause = new IOException(e);
                }
            }
        }
        transactions.clear();
        if (cause != null) {
            throw cause;
        }
    }

    protected DataStoreTransaction getTransaction(Object object) {
        return getTransaction(ClassType.of(object.getClass()));
    }

    protected DataStoreTransaction getTransaction(Type<?> cls) {
        DataStore lookupDataStore = this.multiplexManager.getSubManager(cls);

        DataStoreTransaction transaction = transactions.get(lookupDataStore);
        if (transaction == null) {
            for (DataStore dataStore : multiplexManager.dataStores) {
                if (dataStore.equals(lookupDataStore)) {
                    transaction = beginTransaction(dataStore);
                    transactions.put(dataStore, transaction);
                    break;
                }
            }
            if (transaction == null) {
                throw new InvalidCollectionException(cls.getName());
            }
        }
        return transaction;
    }

    protected DataStoreTransaction getRelationTransaction(Object object, String relationName) {
        EntityDictionary dictionary = multiplexManager.getDictionary();
        Type<?> relationClass = dictionary.getParameterizedType(EntityDictionary.getType(object), relationName);
        return getTransaction(relationClass);
    }

    @Override
    public <T, R> R getRelation(DataStoreTransaction tx,
                              T entity,
                              Relationship relation,
                              RequestScope scope) {

        FilterExpression filter = relation.getProjection().getFilterExpression();

        DataStoreTransaction relationTx = getRelationTransaction(entity, relation.getName());
        Type<Object> entityType = EntityDictionary.getType(entity);
        DataStoreTransaction entityTransaction = getTransaction(entityType);

        EntityDictionary dictionary = scope.getDictionary();
        Type<?> relationClass = dictionary.getParameterizedType(entityType, relation.getName());
        String idFieldName = dictionary.getIdFieldName(relationClass);

        // If different transactions, check if bridgeable and try to bridge
        if (entityTransaction != relationTx && relationTx instanceof BridgeableTransaction) {
            BridgeableTransaction bridgeableTx = (BridgeableTransaction) relationTx;
            RelationshipType relationType = dictionary.getRelationshipType(entityType, relation.getName());
            Serializable id = (filter != null) ? extractId(filter, idFieldName, relationClass) : null;

            if (relationType.isToMany()) {
                return id == null ? (R) bridgeableTx.bridgeableLoadObjects(
                                this, entity, relation.getName(),
                        Optional.ofNullable(filter),
                        Optional.ofNullable(relation.getProjection().getSorting()),
                        Optional.ofNullable(relation.getProjection().getPagination()),
                        scope)
                        : (R) bridgeableTx.bridgeableLoadObject(this, entity, relation.getName(),
                        id, Optional.ofNullable(filter), scope);
            }

            return (R) bridgeableTx.bridgeableLoadObject(this, entity, relation.getName(), id,
                    Optional.ofNullable(filter), scope);

        }

        // Otherwise, rely on existing underlying transaction to call correctly into relationTx
        return entityTransaction.getRelation(relationTx, entity, relation, scope);
    }

    @Override
    public <T, R> void updateToManyRelation(DataStoreTransaction tx,
                                     T entity, String relationName,
                                     Set<R> newRelationships,
                                     Set<R> deletedRelationships,
                                     RequestScope scope) {
        DataStoreTransaction relationTx = getRelationTransaction(entity, relationName);
        DataStoreTransaction entityTransaction = getTransaction(EntityDictionary.getType(entity));
        entityTransaction.updateToManyRelation(relationTx, entity, relationName,
                newRelationships, deletedRelationships, scope);
    }

    @Override
    public <T, R> void updateToOneRelation(DataStoreTransaction tx, T entity,
                                    String relationName, R relationshipValue, RequestScope scope) {
        DataStoreTransaction relationTx = getRelationTransaction(entity, relationName);
        DataStoreTransaction entityTransaction = getTransaction(EntityDictionary.getType(entity));
        entityTransaction.updateToOneRelation(relationTx, entity, relationName, relationshipValue, scope);
    }

    @Override
    public <T, R> R getAttribute(T entity, Attribute attribute, RequestScope scope) {
        DataStoreTransaction transaction = getTransaction(EntityDictionary.getType(entity));
        return transaction.getAttribute(entity, attribute, scope);
    }

    @Override
    public <T> void setAttribute(T entity, Attribute attribute, RequestScope scope) {
        DataStoreTransaction transaction = getTransaction(EntityDictionary.getType(entity));
        transaction.setAttribute(entity, attribute, scope);
    }

    @Override
    public <T> FeatureSupport supportsFiltering(RequestScope scope, Optional<T> parent, EntityProjection projection) {
        Type<?> entityClass = projection.getType();
        return getTransaction(entityClass).supportsFiltering(scope, parent, projection);
    }

    @Override
    public <T> boolean supportsSorting(RequestScope scope, Optional<T> parent, EntityProjection projection) {
        Type<?> entityClass = projection.getType();
        return getTransaction(entityClass).supportsSorting(scope, parent, projection);
    }

    @Override
    public <T> boolean supportsPagination(RequestScope scope, Optional<T> parent, EntityProjection projection) {
        Type<?> entityClass = projection.getType();
        return getTransaction(entityClass).supportsPagination(scope, parent, projection);
    }

    private Serializable extractId(FilterExpression filterExpression,
                                   String idFieldName,
                                   Type<?> relationClass) {
        Collection<FilterPredicate> predicates = filterExpression.accept(new PredicateExtractionVisitor());
        return predicates.stream()
                .filter(p -> p.getEntityType() == relationClass && p.getOperator() == Operator.IN)
                .filter(p -> p.getValues().size() == 1)
                .filter(p -> p.getField().equals(idFieldName))
                .findFirst()
                .map(p -> (Serializable) p.getValues().get(0))
                .orElse(null);
    }

    @Override
    public void cancel(RequestScope scope) {
        transactions.values().forEach(dataStoreTransaction -> dataStoreTransaction.cancel(scope));
    }
}
