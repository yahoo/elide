/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation;

import static com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore.META_DATA_PACKAGE;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.datastores.multiplex.MultiplexManager;

/**
 * Aggregation Manager manager an aggregation data store and its meta data store.
 * The request for meta data store models would be sent to meta data store, and others would be sent to aggregation
 * data store.
 */
public class AggregationManager extends MultiplexManager {
    private final AggregationDataStore aggregationDataStore;

    public AggregationManager(AggregationDataStore aggregationDataStore) {
        super(aggregationDataStore, aggregationDataStore.getMetaDataStore());
        this.aggregationDataStore = aggregationDataStore;
    }

    @Override
    public void populateEntityDictionary(EntityDictionary dictionary) {
        setDictionary(dictionary);
        aggregationDataStore.populateEntityDictionary(dictionary);

        for (Class<?> cls : dictionary.getBindings()) {
            if (META_DATA_PACKAGE.equals(cls.getPackage())) {
                this.dataStoreMap.put(cls, aggregationDataStore.getMetaDataStore());
            } else {
                this.dataStoreMap.put(cls, aggregationDataStore);
            }
        }
    }
}
