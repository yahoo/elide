/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation;

import com.yahoo.elide.core.ArgumentType;
import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.datastores.aggregation.schema.Schema;
import com.yahoo.elide.datastores.aggregation.schema.dimension.TimeDimensionColumn;

/**
 * DataStore that supports Aggregation. Uses {@link QueryEngine} to return results.
 */
public abstract class AggregationDataStore implements DataStore {

    private final QueryEngineFactory queryEngineFactory;
    private QueryEngine queryEngine;

    public AggregationDataStore(QueryEngineFactory queryEngineFactory) {
        this.queryEngineFactory = queryEngineFactory;
    }

    /**
     * Populate an {@link EntityDictionary} and use this dictionary to construct a {@link QueryEngine}.
     * @param dictionary the dictionary
     */
    @Override
    public void populateEntityDictionary(EntityDictionary dictionary) {
        queryEngine = queryEngineFactory.buildQueryEngine(dictionary);

        /* Add 'grain' argument to each TimeDimensionColumn */
        for (Schema schema: queryEngine.getSchemas()) {
            for (TimeDimensionColumn timeDim : schema.getTimeDimensions()) {
                dictionary.addArgumentToAttribute(schema.getEntityClass(), timeDim.getName(),
                        new ArgumentType("grain", String.class));
            }
        }
    }

    @Override
    public DataStoreTransaction beginTransaction() {
        return new AggregationDataStoreTransaction(queryEngine);
    }
}
