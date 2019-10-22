/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.datastores.presto;

import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.datastores.aggregation.AggregationDataStore;
import com.yahoo.elide.datastores.aggregation.QueryEngineFactory;

/**
 * Presto Date Store
 */
public class PrestoDataStore extends AggregationDataStore {
    public PrestoDataStore(QueryEngineFactory queryEngineFactory) {
        super(queryEngineFactory);
    }

    @Override
    public DataStoreTransaction beginTransaction() {
        return new PrestoDataStoreTransaction(getQueryEngine());
    }
}
