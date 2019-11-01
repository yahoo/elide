/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation;

import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;

import lombok.AccessLevel;
import lombok.Getter;

/**
 * DataStore that supports Aggregation. Uses {@link QueryEngine} to return results.
 */
public abstract class AggregationDataStore implements DataStore {

    private final QueryEngineFactory queryEngineFactory;

    @Getter(AccessLevel.PROTECTED)
    private final MetaDataStore metaDataStore;

    private QueryEngine queryEngine;

    public AggregationDataStore(QueryEngineFactory queryEngineFactory, MetaDataStore metaDataStore) {
        this.queryEngineFactory = queryEngineFactory;
        this.metaDataStore = metaDataStore;
    }

    /**
     * Populate an {@link EntityDictionary} and use this dictionary to construct a {@link QueryEngine}.
     * @param dictionary the dictionary
     */
    @Override
    public void populateEntityDictionary(EntityDictionary dictionary) {
        if (dictionary instanceof AggregationDictionary) {
            populateEntityDictionary((AggregationDictionary) dictionary);
        } else {
            throw new IllegalArgumentException("Dictionary doesn't support aggregation.");
        }
    }

    protected void populateEntityDictionary(AggregationDictionary dictionary) {
        metaDataStore.storeMetaData(dictionary);
        queryEngine = queryEngineFactory.buildQueryEngine(dictionary, metaDataStore);
    }

    @Override
    public DataStoreTransaction beginTransaction() {
        return new AggregationDataStoreTransaction(queryEngine);
    }
}
