/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata;

import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.datastore.inmemory.HashMapDataStore;
import com.yahoo.elide.core.exceptions.BadRequestException;
import com.yahoo.elide.core.exceptions.InvalidOperationException;
import com.yahoo.elide.core.request.EntityProjection;
import com.yahoo.elide.core.request.Relationship;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * MetaDataStore transaction handler.
 */
public class MetaDataStoreTransaction implements DataStoreTransaction {

    private static final Function<String, HashMapDataStore> REQUEST_ERROR = new Function<String, HashMapDataStore>() {
        @Override
        public HashMapDataStore apply(String key) {
            throw new BadRequestException("API version " + key + " not found");
        }
    };

    private final Map<String, HashMapDataStore> hashMapDataStores;

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
    public Object getRelation(DataStoreTransaction relationTx, Object entity, Relationship relationship,
                    RequestScope scope) {
        return hashMapDataStores
                        .computeIfAbsent(scope.getApiVersion(), REQUEST_ERROR)
                        .getDictionary()
                        .getValue(entity, relationship.getName(), scope);
    }

    @Override
    public Iterable<Object> loadObjects(EntityProjection projection, RequestScope scope) {
        return hashMapDataStores
                        .computeIfAbsent(scope.getApiVersion(), REQUEST_ERROR)
                        .beginTransaction()
                        .loadObjects(projection, scope);
    }

    @Override
    public Object loadObject(EntityProjection projection, Serializable id, RequestScope scope) {
        return hashMapDataStores
                        .computeIfAbsent(scope.getApiVersion(), REQUEST_ERROR)
                        .beginTransaction()
                        .loadObject(projection, id, scope);
    }

    @Override
    public void close() throws IOException {
        // Do nothing
    }

    @Override
    public FeatureSupport supportsFiltering(RequestScope scope, Optional<Object> parent, EntityProjection projection) {
        return FeatureSupport.NONE;
    }

    @Override
    public boolean supportsSorting(RequestScope scope, Optional<Object> parent, EntityProjection projection) {
        return false;
    }

    @Override
    public boolean supportsPagination(RequestScope scope, Optional<Object> parent, EntityProjection projection) {
        return false;
    }

    @Override
    public void cancel(RequestScope scope) {
        // Do nothing
    }
}
