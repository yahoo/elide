/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastore.multiplex;

import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.exceptions.TransactionException;

import com.google.common.collect.Lists;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map.Entry;

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
    public void save(Object entity) {
        getTransaction(entity).save(entity);
        dirtyObjects.add(this.multiplexManager.getSubManager(entity.getClass()), entity);
    }

    @Override
    public void delete(Object entity) {
        getTransaction(entity).delete(entity);
        dirtyObjects.add(this.multiplexManager.getSubManager(entity.getClass()), entity);
    }

    @Override
    public void commit() {
        // flush all before commits
        flush();

        ArrayList<DataStore> commitList = new ArrayList<>();
        for (Entry<DataStore, DataStoreTransaction> entry : transactions.entrySet()) {
            try {
                entry.getValue().commit();
                commitList.add(entry.getKey());
            } catch (Error | RuntimeException e) {
                TransactionException transactionException = new TransactionException(e);
                reverseTransactions(commitList, transactionException);
                throw transactionException;
            }
        }
        transactions.clear();
    }

    /**
     * Attempt to reverse changes of last commit since not all transactions successfully committed.
     * @param restoreList List of database managers to reverse the last commit
     * @param cause cause to add any suppressed exceptions
     */
    private void reverseTransactions(ArrayList<DataStore> restoreList, Throwable cause) {
        for (DataStore dataStore : restoreList) {
            try (DataStoreTransaction tx = dataStore.beginTransaction()) {
                List<Object> list = dirtyObjects.get(dataStore);
                for (Object dirtyObject : list == null ? Collections.emptyList() : list) {
                    Object cloned = clonedObjects.get(dirtyObject);
                    if (cloned == NEWLY_CREATED_OBJECT) {
                        tx.delete(dirtyObject);
                    } else {
                        tx.save(cloned);
                    }
                }
                tx.commit();
            } catch (RuntimeException | IOException e) {
                cause.addSuppressed(e);
            }
        }
    }

    @SuppressWarnings("resource")
    @Override
    public <T> T createObject(Class<T> createObject) {
        DataStoreTransaction transaction = getTransaction(createObject);
        T object = transaction.createObject(createObject);
        // mark this object as newly created to be deleted on reverse transaction
        clonedObjects.put(object, NEWLY_CREATED_OBJECT);
        return object;

    }

    private <T> Iterable<T> hold(DataStoreTransaction transaction, Iterable<T> list) {
        if (transaction != lastDataStoreTransaction) {
            ArrayList<T> newList = Lists.newArrayList(list);
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
        EntityDictionary dictionary = this.multiplexManager.getDictionary();
        Class<?> cls = dictionary.lookupEntityClass(object.getClass());
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
    public <T> T loadObject(Class<T> loadClass, String id) {
        DataStoreTransaction transaction = getTransaction(loadClass);
        return hold(transaction, transaction.loadObject(loadClass, id));
    }

    @Override
    public <T> Iterable<T> loadObjects(Class<T> loadClass) {
        DataStoreTransaction transaction = getTransaction(loadClass);
        return hold(transaction, transaction.loadObjects(loadClass));
    }
}
