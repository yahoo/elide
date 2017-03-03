/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.multiplex;

import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.RelationshipType;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.exceptions.InvalidCollectionException;
import com.yahoo.elide.core.filter.FilterPredicate;
import com.yahoo.elide.core.filter.Operator;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.expression.PredicateExtractionVisitor;
import com.yahoo.elide.core.pagination.Pagination;
import com.yahoo.elide.core.sort.Sorting;
import com.yahoo.elide.security.User;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Multiplex transaction handler.  Process each sub-database transactions within a single transaction.
 * If any commit fails in process, reverse any commits already completed.
 */
@Slf4j
public abstract class MultiplexTransaction implements DataStoreTransaction {
    protected final LinkedHashMap<DataStore, DataStoreTransaction> transactions;
    protected final MultiplexManager multiplexManager;
    protected final DataStoreTransaction lastDataStoreTransaction;

    /**
     * Multiplex transaction handler.
     * @param multiplexManager associated manager
     */
    public MultiplexTransaction(MultiplexManager multiplexManager) {
        this.multiplexManager = multiplexManager;
        this.transactions = new LinkedHashMap<>(multiplexManager.dataStores.size());

        // create each subordinate transaction
        DataStoreTransaction transaction = null;
        for (DataStore dataStore : multiplexManager.dataStores) {
            transaction = beginTransaction(dataStore);
            transactions.put(dataStore, transaction);
        }
        lastDataStoreTransaction = transaction;
    }

    protected abstract DataStoreTransaction beginTransaction(DataStore dataStore);

    @Override
    public User accessUser(Object opaqueUser) {
        User user = new User(opaqueUser);
        for (DataStore dataStore : multiplexManager.dataStores) {
            DataStoreTransaction transaction = transactions.get(dataStore);
            user = transaction.accessUser(user.getOpaqueUser());
        }
        return user;
    }

    @Override
    public void createObject(Object entity, RequestScope scope) {
        getTransaction(entity).createObject(entity, scope);
    }


    @Override
    public Object loadObject(Class<?> entityClass,
                             Serializable id,
                             Optional<FilterExpression> filterExpression,
                             RequestScope scope) {
        return getTransaction(entityClass).loadObject(entityClass, id, filterExpression, scope);
    }

    @Override
    public Iterable<Object> loadObjects(
            Class<?> entityClass,
            Optional<FilterExpression> filterExpression,
            Optional<Sorting> sorting,
            Optional<Pagination> pagination,
            RequestScope scope) {
        return getTransaction(entityClass).loadObjects(entityClass,
                filterExpression,
                sorting,
                pagination,
                scope);
    }

    @Override
    public void flush(RequestScope requestScope) {
        transactions.values().forEach(dataStoreTransaction -> dataStoreTransaction.flush(requestScope));
    }

    @Override
    public void preCommit() {
        transactions.values().forEach(DataStoreTransaction::preCommit);
    }

    @Override
    public void commit(RequestScope requestScope) {
        // flush all before commit
        flush(requestScope);
        transactions.values().forEach(dataStoreTransaction -> dataStoreTransaction.commit(requestScope));
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
        DataStoreTransaction transaction = transactions.get(this.multiplexManager.getSubManager(cls));
        if (transaction == null) {
            Class entityClass = multiplexManager.getDictionary().lookupEntityClass(cls);
            throw new InvalidCollectionException(entityClass == null ? cls.getName() : entityClass.getName());
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
                              String relationName,
                              Optional<FilterExpression> filterExpression,
                              Optional<Sorting> sorting,
                              Optional<Pagination> pagination,
                              RequestScope scope) {
        relationTx = getRelationTransaction(entity, relationName);
        DataStoreTransaction entityTransaction = getTransaction(entity.getClass());

        EntityDictionary dictionary = scope.getDictionary();
        Class<?> relationClass = dictionary.getParameterizedType(entity, relationName);
        String idFieldName = dictionary.getIdFieldName(relationClass);

        // If different transactions, check if bridgeable and try to bridge
        if (entityTransaction != relationTx && relationTx instanceof BridgeableTransaction) {
            RelationshipType relationType = dictionary.getRelationshipType(entity.getClass(), relationName);
            BridgeableTransaction bridgeableTransaction = (BridgeableTransaction) relationTx;
            if (relationType.isToMany()) {
                return filterExpression
                        .map(fe -> {
                            Serializable id = extractId(fe, idFieldName, entity, relationClass, dictionary);
                            if (id == null) {
                                return bridgeableTransaction.bridgeableLoadObjects(this,
                                        entity, relationName, filterExpression, sorting, pagination, scope);
                            } else {
                                return bridgeableTransaction.bridgeableLoadObject(this,
                                        entity, relationName, id, filterExpression, scope);
                            }
                        })
                        .orElseGet(() -> bridgeableTransaction.bridgeableLoadObjects(this,
                        entity, relationName, filterExpression, sorting, pagination, scope));
            } else {
                return filterExpression
                        .map(fe -> {
                            Serializable id = extractId(fe, idFieldName, entity, relationClass, dictionary);
                            return bridgeableTransaction.bridgeableLoadObject(this,
                                    entity, relationName, id, filterExpression, scope);
                        })
                        .orElseGet(() -> bridgeableTransaction.bridgeableLoadObject(this,
                                entity, relationName, null, filterExpression, scope));
            }
        }

        // Otherwise, rely on existing underlying transaction to call correctly into relationTx
        return entityTransaction.getRelation(relationTx, entity,
                relationName, filterExpression, sorting, pagination, scope);
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
        entityTransaction.updateToOneRelation(relationTx, entity, relationName,
                relationshipValue, scope);
    }

    @Override
    public Object getAttribute(Object entity,
                               String attributeName, RequestScope scope) {
        DataStoreTransaction transaction = getTransaction(entity.getClass());
        return transaction.getAttribute(entity, attributeName, scope);
    }

    @Override
    public void setAttribute(Object entity, String attributeName, Object attributeValue, RequestScope scope) {
        DataStoreTransaction transaction = getTransaction(entity.getClass());
        transaction.setAttribute(entity, attributeName, attributeValue, scope);
    }

    @Override
    public <T> Long getTotalRecords(Class<T> entityClass) {
        return getTransaction(entityClass).getTotalRecords(entityClass);
    }

    private Serializable extractId(FilterExpression filterExpression,
                                   String idFieldName,
                                   Object parent,
                                   Class<?> relationClass,
                                   EntityDictionary dictionary) {
        Set<FilterPredicate> predicates = filterExpression.accept(new PredicateExtractionVisitor());
        for (FilterPredicate predicate : predicates) {
            List<Object> values = predicate.getValues();
            Class<?> entityClass = dictionary.getParameterizedType(parent, predicate.getLeafEntityType());
            if (relationClass == entityClass
                    && predicate.getOperator() == Operator.IN
                    && idFieldName.equals(predicate.getField())
                    && values.size() == 1) {
                return (Serializable) values.get(0);
            }
        }
        return null;
    }
}
