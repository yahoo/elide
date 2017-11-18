/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.multiplex;

import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.exceptions.HttpStatusException;
import com.yahoo.elide.core.exceptions.TransactionException;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.pagination.Pagination;
import com.yahoo.elide.core.sort.Sorting;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MultivaluedHashMap;

/**
 * Multiplex transaction handler.
 */
public class MultiplexWriteTransaction extends MultiplexTransaction {
    private static final Object NEWLY_CREATED_OBJECT = new Object();
    private final IdentityHashMap<Object, Object> clonedObjects = new IdentityHashMap<>();
    private final MultivaluedHashMap<DataStore, Object> dirtyObjects = new MultivaluedHashMap<>();

    public MultiplexWriteTransaction(MultiplexManager multiplexManager) {
        super(multiplexManager);
    }

    @Override
    protected DataStoreTransaction beginTransaction(DataStore dataStore) {
        // begin updating transaction
        return dataStore.beginTransaction();
    }

    @Override
    public void save(Object entity, RequestScope requestScope) {
        getTransaction(entity).save(entity, requestScope);
        dirtyObjects.add(this.multiplexManager.getSubManager(entity.getClass()), entity);
    }

    @Override
    public void delete(Object entity, RequestScope requestScope) {
        getTransaction(entity).delete(entity, requestScope);
        dirtyObjects.add(this.multiplexManager.getSubManager(entity.getClass()), entity);
    }

    @Override
    public void commit(RequestScope scope) {
        // flush all before commits
        flush(scope);

        ArrayList<DataStore> commitList = new ArrayList<>();
        for (Entry<DataStore, DataStoreTransaction> entry : transactions.entrySet()) {
            try {
                entry.getValue().commit(scope);
                commitList.add(entry.getKey());
            } catch (HttpStatusException | WebApplicationException e) {
                reverseTransactions(commitList, e, scope);
                throw e;
            } catch (Error | RuntimeException e) {
                TransactionException transactionException = new TransactionException(e);
                reverseTransactions(commitList, transactionException, scope);
                throw transactionException;
            }
        }
    }

    /**
     * Attempt to reverse changes of last commit since not all transactions successfully committed.
     * @param restoreList List of database managers to reverse the last commit
     * @param cause cause to add any suppressed exceptions
     */
    private void reverseTransactions(ArrayList<DataStore> restoreList, Throwable cause, RequestScope requestScope) {
        for (DataStore dataStore : restoreList) {
            try (DataStoreTransaction transaction = dataStore.beginTransaction()) {
                List<Object> list = dirtyObjects.get(dataStore);
                for (Object dirtyObject : list == null ? Collections.emptyList() : list) {
                    Object cloned = clonedObjects.get(dirtyObject);
                    if (cloned == NEWLY_CREATED_OBJECT) {
                        transaction.delete(dirtyObject, requestScope);
                    } else {
                        transaction.save(cloned, requestScope);
                    }
                }
                transaction.commit(requestScope);
            } catch (RuntimeException | IOException e) {
                cause.addSuppressed(e);
            }
        }
    }

    @SuppressWarnings("resource")
    @Override
    public void createObject(Object entity, RequestScope scope) {
        DataStoreTransaction transaction = getTransaction(entity.getClass());
        transaction.createObject(entity, scope);
        // mark this object as newly created to be deleted on reverse transaction
        clonedObjects.put(entity, NEWLY_CREATED_OBJECT);
    }

    private <T> Iterable<T> hold(DataStoreTransaction transaction, Iterable<T> list) {
        if (transaction != lastDataStoreTransaction) {
            ArrayList<T> newList = new ArrayList<>();
            list.forEach(newList::add);
            for (T object : newList) {
                hold(transaction, object);
            }
            return newList;
        }
        return list;
    }

    /**
     * Save cloned copy of object for possible reverse transaction.
     * @param subTransaction database sub-transaction
     * @param object entity to clone
     * @return original object
     */
    private <T> T hold(DataStoreTransaction subTransaction, T object) {
        if (subTransaction != lastDataStoreTransaction) {
            clonedObjects.put(object, cloneObject(object));
        }
        return object;
    }

    /**
     *  Clone contents of object for possible reverse transaction.
     */
    private Object cloneObject(Object object) {
        if (object == null) {
            return null;
        }

        Class<?> cls = multiplexManager.getDictionary().lookupEntityClass(object.getClass());
        try {
            Object clone = cls.newInstance();
            for (Field field : cls.getFields()) {
                field.set(clone, field.get(object));
            }
            for (Method method : cls.getMethods()) {
                if (method.getName().startsWith("set")) {
                    try {
                        Method getMethod = cls.getMethod("get" + method.getName().substring(3));
                        method.invoke(clone, getMethod.invoke(object));
                    } catch (IllegalStateException | IllegalArgumentException
                            | ReflectiveOperationException | SecurityException e) {
                        return null;
                    }
                }
            }
            return clone;
        } catch (InstantiationException | IllegalAccessException e) {
            // ignore
        }
        return null;
    }

    @Override
    public Object loadObject(Class<?> entityClass,
                             Serializable id,
                             Optional<FilterExpression> filterExpression,
                             RequestScope scope) {
        DataStoreTransaction transaction = getTransaction(entityClass);
        return hold(transaction, transaction.loadObject(entityClass, id, filterExpression, scope));
    }

    @Override
    public Iterable<Object> loadObjects(
            Class<?> entityClass,
            Optional<FilterExpression> filterExpression,
            Optional<Sorting> sorting,
            Optional<Pagination> pagination,
            RequestScope scope) {
        DataStoreTransaction transaction = getTransaction(entityClass);
        return hold(transaction, transaction.loadObjects(entityClass, filterExpression, sorting, pagination, scope));
    }

    @Override
    public Object getRelation(DataStoreTransaction relationTx,
                              Object entity,
                              String relationName,
                              Optional<FilterExpression> filter,
                              Optional<Sorting> sorting,
                              Optional<Pagination> pagination,
                              RequestScope scope) {
        DataStoreTransaction transaction = getTransaction(entity.getClass());
        Object relation = super.getRelation(relationTx, entity, relationName,
                filter, sorting, pagination, scope);

        if (relation instanceof Iterable) {
            return hold(transaction, (Iterable) relation);
        }

        return hold(transaction, relation);
    }
}
