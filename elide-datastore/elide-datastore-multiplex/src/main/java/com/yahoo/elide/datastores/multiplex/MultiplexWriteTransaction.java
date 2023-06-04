/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.multiplex;

import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.datastore.DataStore;
import com.yahoo.elide.core.datastore.DataStoreIterable;
import com.yahoo.elide.core.datastore.DataStoreIterableBuilder;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.exceptions.HttpStatusException;
import com.yahoo.elide.core.exceptions.TransactionException;
import com.yahoo.elide.core.request.EntityProjection;
import com.yahoo.elide.core.request.Relationship;
import com.yahoo.elide.core.type.Type;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MultivaluedHashMap;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map.Entry;

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
    public <T> void save(T entity, RequestScope requestScope) {
        Type<Object> entityType = EntityDictionary.getType(entity);
        getTransaction(entityType).save(entity, requestScope);
        dirtyObjects.add(this.multiplexManager.getSubManager(entityType), entity);
    }

    @Override
    public <T> void delete(T entity, RequestScope requestScope) {
        Type<Object> entityType = EntityDictionary.getType(entity);
        getTransaction(entityType).delete(entity, requestScope);
        dirtyObjects.add(this.multiplexManager.getSubManager(entityType), entity);
    }

    @Override
    public void commit(RequestScope scope) {
        // flush all before commits
        flush(scope);

        List<DataStore> commitList = new ArrayList<>();

        // Transactions must be committed in reverse order
        ListIterator<Entry<DataStore, DataStoreTransaction>> iterator = new ArrayList<>(transactions.entrySet())
                .listIterator(transactions.size());
        while (iterator.hasPrevious()) {
            Entry<DataStore, DataStoreTransaction> entry = iterator.previous();
            try {
                entry.getValue().commit(scope);
                if (this.multiplexManager.isApplyCompensatingTransactions(entry.getKey())) {
                    commitList.add(entry.getKey());
                }
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
    private void reverseTransactions(List<DataStore> restoreList, Throwable cause, RequestScope requestScope) {
        for (DataStore dataStore : restoreList) {
            try (DataStoreTransaction transaction = dataStore.beginTransaction()) {
                List<Object> list = dirtyObjects.get(dataStore);
                for (Object dirtyObject : list == null ? Collections.emptyList() : list) {
                    Object cloned = clonedObjects.get(dirtyObject);
                    if (cloned == NEWLY_CREATED_OBJECT) {
                        transaction.delete(dirtyObject, requestScope);
                    } else {
                        // If cloned is null this is an update to an object that wasn't created yet
                        if (cloned != null) {
                            transaction.save(cloned, requestScope);
                        }
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
    public <T> void createObject(T entity, RequestScope scope) {
        DataStoreTransaction transaction = getTransaction(EntityDictionary.getType(entity));
        transaction.createObject(entity, scope);
        // mark this object as newly created to be deleted on reverse transaction
        clonedObjects.put(entity, NEWLY_CREATED_OBJECT);
    }

    private <T> DataStoreIterable<T> hold(DataStoreTransaction transaction, DataStoreIterable<T> list) {
        ArrayList<T> newList = new ArrayList<>();
        list.forEach(newList::add);
        for (T object : newList) {
            hold(transaction, object);
        }
        return new DataStoreIterableBuilder<T>(newList)
                .paginateInMemory(list.needsInMemoryPagination())
                .filterInMemory(list.needsInMemoryFilter())
                .sortInMemory(list.needsInMemorySort())
                .build();
    }

    /**
     * Save cloned copy of object for possible reverse transaction.
     * @param subTransaction database sub-transaction
     * @param object entity to clone
     * @return original object
     */
    private <T> T hold(DataStoreTransaction subTransaction, T object) {
        clonedObjects.put(object, cloneObject(object));
        return object;
    }

    /**
     *  Clone contents of object for possible reverse transaction.
     */
    private Object cloneObject(Object object) {
        if (object == null) {
            return null;
        }

        Type<?> cls = multiplexManager.getDictionary().lookupBoundClass(EntityDictionary.getType(object));
        return this.multiplexManager.objectCloner.clone(object, cls);
    }

    @Override
    public <T> T loadObject(EntityProjection projection,
                             Serializable id,
                             RequestScope scope) {
        DataStoreTransaction transaction = getTransaction(projection.getType());
        return hold(transaction, (T) transaction.loadObject(projection, id, scope));
    }

    @Override
    public <T> DataStoreIterable<T> loadObjects(EntityProjection projection, RequestScope scope) {
        DataStoreTransaction transaction = getTransaction(projection.getType());
        return hold(transaction, transaction.loadObjects(projection, scope));
    }

    @Override
    public <T, R> DataStoreIterable<R> getToManyRelation(DataStoreTransaction relationTx,
                                                         T entity,
                                                         Relationship relationship,
                                                         RequestScope scope) {
        DataStoreTransaction transaction = getTransaction(EntityDictionary.getType(entity));
        DataStoreIterable<R> relation = super.getToManyRelation(relationTx, entity, relationship, scope);

        return hold(transaction, relation);
    }

    @Override
    public <T, R> R getToOneRelation(DataStoreTransaction relationTx,
                                     T entity,
                                     Relationship relationship,
                                     RequestScope scope) {
        DataStoreTransaction transaction = getTransaction(EntityDictionary.getType(entity));
        R relation = super.getToOneRelation(relationTx, entity, relationship, scope);

        return hold(transaction, relation);
    }
}
