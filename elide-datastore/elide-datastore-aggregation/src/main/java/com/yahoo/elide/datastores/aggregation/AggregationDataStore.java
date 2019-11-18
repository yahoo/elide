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
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import com.yahoo.elide.datastores.aggregation.metadata.models.AnalyticView;
import com.yahoo.elide.datastores.aggregation.metadata.models.TimeDimension;

import lombok.Getter;

/**
 * DataStore that supports Aggregation. Uses {@link QueryEngine} to return results.
 */
public abstract class AggregationDataStore implements DataStore {

    private final QueryEngineFactory queryEngineFactory;

    @Getter
    private final MetaDataStore metaDataStore;

    private QueryEngine queryEngine;

    public AggregationDataStore(QueryEngineFactory queryEngineFactory) {
        this.queryEngineFactory = queryEngineFactory;
        this.metaDataStore = new MetaDataStore();
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
        metaDataStore.populateEntityDictionary(dictionary);
        queryEngine = queryEngineFactory.buildQueryEngine(dictionary, metaDataStore);

        /* Add 'grain' argument to each TimeDimensionColumn */
        for (AnalyticView table : metaDataStore.getMetaData(AnalyticView.class)) {
            for (TimeDimension timeDim : table.getColumns(TimeDimension.class)) {
                dictionary.addArgumentToAttribute(
                        table.getCls(),
                        timeDim.getName(),
                        new ArgumentType("grain", String.class));
            }
        }
    }

    @Override
    public DataStoreTransaction beginTransaction() {
        return new AggregationDataStoreTransaction(queryEngine);
    }
}
