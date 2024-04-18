/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.datastores.aggregation.metadata;

import com.paiondata.elide.core.RequestScope;
import com.paiondata.elide.core.datastore.DataStore;
import com.paiondata.elide.core.datastore.DataStoreIterable;
import com.paiondata.elide.core.datastore.DataStoreTransaction;
import com.paiondata.elide.core.datastore.inmemory.HashMapDataStore;
import com.paiondata.elide.core.exceptions.BadRequestException;
import com.paiondata.elide.core.exceptions.InvalidOperationException;
import com.paiondata.elide.core.request.EntityProjection;
import com.paiondata.elide.core.request.Relationship;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * MetaDataStore transaction handler.
 */
public class MetaDataStoreTransaction implements DataStoreTransaction {

    private static final Function<String, HashMapDataStore> REQUEST_ERROR = key -> {
        throw new BadRequestException("API version " + key + " not found");
    };

    private final Map<String, HashMapDataStore> hashMapDataStores;

    private final Map<String, DataStoreTransaction> transactions = new HashMap<>();

    public MetaDataStoreTransaction(Map<String, HashMapDataStore> hashMapDataStores) {
        this.hashMapDataStores = hashMapDataStores;
    }

    @Override
    public void flush(RequestScope requestScope) {
        // Do nothing
    }

    @Override
    public void save(Object object, RequestScope requestScope) {
        throw new InvalidOperationException("save not supported for metadatastore");
    }

    @Override
    public void delete(Object object, RequestScope requestScope) {
        throw new InvalidOperationException("delete not supported for metadatastore");
    }

    @Override
    public void commit(RequestScope scope) {
        // Do nothing
    }

    @Override
    public void createObject(Object entity, RequestScope scope) {
        // Do nothing
    }

    @Override
    public DataStoreIterable<Object> getToManyRelation(
            DataStoreTransaction relationTx,
            Object entity,
            Relationship relationship,
            RequestScope scope
    ) {
        DataStoreTransaction dataStoreTransaction = getTransaction(scope);
        return dataStoreTransaction.getToManyRelation(relationTx, entity, relationship, scope);
    }

    @Override
    public Object getToOneRelation(
            DataStoreTransaction relationTx,
            Object entity,
            Relationship relationship,
            RequestScope scope
    ) {
        DataStoreTransaction dataStoreTransaction = getTransaction(scope);
        return dataStoreTransaction.getToOneRelation(relationTx, entity, relationship, scope);
    }

    @Override
    public DataStoreIterable<Object> loadObjects(EntityProjection projection, RequestScope scope) {
        DataStoreTransaction dataStoreTransaction = getTransaction(scope);
        return dataStoreTransaction.loadObjects(projection, scope);
    }

    @Override
    public Object loadObject(EntityProjection projection, Serializable id, RequestScope scope) {
        DataStoreTransaction dataStoreTransaction = getTransaction(scope);
        return dataStoreTransaction.loadObject(projection, id, scope);
    }

    protected DataStoreTransaction getTransaction(RequestScope scope) {
        DataStore dataStore = hashMapDataStores.computeIfAbsent(scope.getRoute().getApiVersion(), REQUEST_ERROR);
        return transactions.computeIfAbsent(scope.getRoute().getApiVersion(),
                key -> dataStore.beginReadTransaction());
    }

    @Override
    public void close() throws IOException {
        IOException exception = null;
        for (DataStoreTransaction transaction : transactions.values()) {
            try {
                transaction.close();
            } catch (IOException e) {
                if (exception == null) {
                    exception = e;
                } else {
                    exception.addSuppressed(e);
                }
            } catch (RuntimeException e) {
                if (exception == null) {
                    exception = new IOException(e);
                } else {
                    exception.addSuppressed(e);
                }
            }
        }
        transactions.clear();
        if (exception != null) {
            throw exception;
        }
    }

    @Override
    public void cancel(RequestScope scope) {
        // Do nothing
    }
}
