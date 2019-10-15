/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation;

import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import lombok.Getter;
import lombok.Setter;

/**
 * DataStore that supports Aggregation. Uses {@link QueryEngine} to return results.
 */
public abstract class AggregationDataStore implements DataStore {

    private QueryEngineFactory queryEngineFactory;
    private QueryEngine queryEngine;

    @Getter
    @Setter
    private EntityDictionary dictionary;

    public AggregationDataStore(QueryEngineFactory queryEngineFactory) {
        this.queryEngineFactory = queryEngineFactory;
        queryEngine = null;
    }

    @Override
    public abstract void populateEntityDictionary(EntityDictionary dictionary);

    @Override
    public DataStoreTransaction beginTransaction() {
        if (queryEngine == null) {
            queryEngine = queryEngineFactory.buildQueryEngine(dictionary);
        }
        return new AggregationDataStoreTransaction(queryEngine);
    }
}
