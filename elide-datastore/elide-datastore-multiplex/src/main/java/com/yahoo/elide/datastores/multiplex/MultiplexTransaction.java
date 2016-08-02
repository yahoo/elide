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
import com.yahoo.elide.core.filter.Predicate;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.pagination.Pagination;
import com.yahoo.elide.core.sort.Sorting;
import com.yahoo.elide.security.User;

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
    @Deprecated
    public <T> Collection filterCollection(Collection collection, Class<T> entityClass, Set<Predicate> predicates) {
        return getTransaction(entityClass).filterCollection(collection, entityClass, predicates);
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
        transactions.clear();
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

    @Override
    public <T> Object getRelation(
            Object entity,
            RelationshipType relationshipType,
            String relationName,
            Class<T> relationClass,
            EntityDictionary dictionary,
            Set<Predicate> filters
    ) {
        DataStoreTransaction transaction = getTransaction(entity.getClass());
        return transaction.getRelation(entity, relationshipType, relationName, relationClass, dictionary, filters);
    }

    @Override
    public <T> Object getRelationWithSortingAndPagination(
            Object entity,
            RelationshipType relationshipType,
            String relationName,
            Class<T> relationClass,
            EntityDictionary dictionary,
            Set<Predicate> filters,
            Sorting sorting,
            Pagination pagination
    ) {
        DataStoreTransaction transaction = getTransaction(entity.getClass());
        return transaction.getRelationWithSortingAndPagination(entity, relationshipType, relationName,
                relationClass, dictionary, filters, sorting, pagination);
    }

//    @Override
//    public <T> Iterable<T> loadObjectsWithSortingAndPagination(Class<T> entityClass, FilterScope filterScope) {
//        return getTransaction(entityClass).loadObjectsWithSortingAndPagination(entityClass, filterScope);
//    }

    @Override
    public <T> Collection filterCollectionWithSortingAndPagination(Collection collection, Class<T> entityClass,
            EntityDictionary dictionary, Optional<Set<Predicate>> filters, Optional<Sorting> sorting,
            Optional<Pagination> pagination) {
        return getTransaction(entityClass).filterCollectionWithSortingAndPagination(
                collection, entityClass, dictionary, filters, sorting, pagination);
    }

    @Override
    public <T> Long getTotalRecords(Class<T> entityClass) {
        return getTransaction(entityClass).getTotalRecords(entityClass);
    }
}
