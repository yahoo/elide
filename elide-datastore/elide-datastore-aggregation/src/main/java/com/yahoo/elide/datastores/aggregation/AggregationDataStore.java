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
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromSubquery;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromTable;
import com.yahoo.elide.utils.ClassScanner;

import java.lang.annotation.Annotation;

/**
 * DataStore that supports Aggregation. Uses {@link QueryEngine} to return results.
 */
public class AggregationDataStore implements DataStore {

    private final QueryEngineFactory queryEngineFactory;

    private final MetaDataStore metaDataStore;

    private QueryEngine queryEngine;

    private final String modelPackageName;

    /**
     * These are the classes the Aggregation Store manages.
     */
    private static final Class[] AGGREGATION_STORE_CLASSES = { FromTable.class, FromSubquery.class };

    public AggregationDataStore(QueryEngineFactory queryEngineFactory,
                                MetaDataStore metaDataStore) {
        this.queryEngineFactory = queryEngineFactory;
        this.metaDataStore = metaDataStore;
        this.modelPackageName = metaDataStore.getScanPackageName();
    }

    /**
     * Populate an {@link EntityDictionary} and use this dictionary to construct a {@link QueryEngine}.
     * @param dictionary the dictionary
     */
    @Override
    public void populateEntityDictionary(EntityDictionary dictionary) {
        for (Class<? extends Annotation> cls : AGGREGATION_STORE_CLASSES) {
            ClassScanner.getAnnotatedClasses(modelPackageName, cls).stream().forEach(modelClass -> {
                dictionary.bindEntity(modelClass);
            });
        }

        queryEngine = queryEngineFactory.buildQueryEngine(metaDataStore);

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
