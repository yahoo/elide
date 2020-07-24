/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.multiplex;

import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.RelationshipType;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.exceptions.InvalidCollectionException;
import com.yahoo.elide.core.filter.FilterPredicate;
import com.yahoo.elide.core.filter.Operator;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.expression.PredicateExtractionVisitor;
import com.yahoo.elide.request.Attribute;
import com.yahoo.elide.request.EntityProjection;
import com.yahoo.elide.request.Pagination;
import com.yahoo.elide.request.Relationship;
import com.yahoo.elide.request.Sorting;

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
    public void createObject(Object entity, RequestScope scope) {
        getTransaction(entity).createObject(entity, scope);
    }

    @Override
    public Object loadObject(EntityProjection projection,
                             Serializable id,
                             RequestScope scope) {
        return getTransaction(projection.getType()).loadObject(projection, id, scope);
    }

    @Override
    public Iterable<Object> loadObjects(
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
    public void preCommit() {
        transactions.values().stream()
                .filter(dataStoreTransaction -> dataStoreTransaction != null)
                .forEach(DataStoreTransaction::preCommit);
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
        return getTransaction(object.getClass());
    }

    protected DataStoreTransaction getTransaction(Class<?> cls) {
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
        Class<?> relationClass = dictionary.getParameterizedType(object, relationName);
        return getTransaction(relationClass);
    }

    @Override
    public Object getRelation(DataStoreTransaction relationTx,
                              Object entity,
                              Relationship relation,
                              RequestScope scope) {

        FilterExpression filter = relation.getProjection().getFilterExpression();
        Sorting sorting = relation.getProjection().getSorting();
        Pagination pagination = relation.getProjection().getPagination();

        relationTx = getRelationTransaction(entity, relation.getName());
        DataStoreTransaction entityTransaction = getTransaction(entity.getClass());

        EntityDictionary dictionary = scope.getDictionary();
        Class<?> relationClass = dictionary.getParameterizedType(entity, relation.getName());
        String idFieldName = dictionary.getIdFieldName(relationClass);

        // If different transactions, check if bridgeable and try to bridge
        if (entityTransaction != relationTx && relationTx instanceof BridgeableTransaction) {
            BridgeableTransaction bridgeableTx = (BridgeableTransaction) relationTx;
            RelationshipType relationType = dictionary.getRelationshipType(entity.getClass(), relation.getName());
            Serializable id = (filter != null) ? extractId(filter, idFieldName, relationClass) : null;

            if (relationType.isToMany()) {
                return id == null ? bridgeableTx.bridgeableLoadObjects(
                                this, entity, relation.getName(),
                        Optional.ofNullable(filter),
                        Optional.ofNullable(relation.getProjection().getSorting()),
                        Optional.ofNullable(relation.getProjection().getPagination()),
                        scope)
                        : bridgeableTx.bridgeableLoadObject(this, entity, relation.getName(),
                        id, Optional.ofNullable(filter), scope);
            }

            return bridgeableTx.bridgeableLoadObject(this, entity, relation.getName(), id,
                    Optional.ofNullable(filter), scope);

        }

        // Otherwise, rely on existing underlying transaction to call correctly into relationTx
        return entityTransaction.getRelation(relationTx, entity, relation, scope);
    }

    @Override
    public void updateToManyRelation(DataStoreTransaction relationTx,
                                     Object entity, String relationName,
                                     Set<Object> newRelationships,
                                     Set<Object> deletedRelationships,
                                     RequestScope scope) {
        relationTx = getRelationTransaction(entity, relationName);
        DataStoreTransaction entityTransaction = getTransaction(entity.getClass());
        entityTransaction.updateToManyRelation(relationTx, entity, relationName,
                newRelationships, deletedRelationships, scope);
    }

    @Override
    public void updateToOneRelation(DataStoreTransaction relationTx, Object entity,
                                    String relationName, Object relationshipValue, RequestScope scope) {
        relationTx = getRelationTransaction(entity, relationName);
        DataStoreTransaction entityTransaction = getTransaction(entity.getClass());
        entityTransaction.updateToOneRelation(relationTx, entity, relationName, relationshipValue, scope);
    }

    @Override
    public Object getAttribute(Object entity, Attribute attribute, RequestScope scope) {
        DataStoreTransaction transaction = getTransaction(entity.getClass());
        return transaction.getAttribute(entity, attribute, scope);
    }

    @Override
    public void setAttribute(Object entity, Attribute attribute, RequestScope scope) {
        DataStoreTransaction transaction = getTransaction(entity.getClass());
        transaction.setAttribute(entity, attribute, scope);
    }

    @Override
    public FeatureSupport supportsFiltering(Class<?> entityClass, FilterExpression expression) {
        return getTransaction(entityClass).supportsFiltering(entityClass, expression);
    }

    @Override
    public boolean supportsSorting(Class<?> entityClass, Sorting sorting) {
        return getTransaction(entityClass).supportsSorting(entityClass, sorting);
    }

    @Override
    public boolean supportsPagination(Class<?> entityClass, FilterExpression expression) {
        return getTransaction(entityClass).supportsPagination(entityClass, expression);
    }

    private Serializable extractId(FilterExpression filterExpression,
                                   String idFieldName,
                                   Class<?> relationClass) {
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
