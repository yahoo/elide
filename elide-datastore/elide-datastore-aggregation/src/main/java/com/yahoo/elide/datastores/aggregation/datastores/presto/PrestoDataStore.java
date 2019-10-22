/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.datastores.presto;

import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.datastores.aggregation.AggregationDataStore;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.SQLQueryEngine;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.SQLQueryEngineFactory;

/**
 * Presto Date Store
 */
public abstract class PrestoDataStore extends AggregationDataStore {
    public PrestoDataStore(SQLQueryEngineFactory queryEngineFactory) {
        super(queryEngineFactory);
    }

    @Override
    public DataStoreTransaction beginTransaction() {
        return new PrestoDataStoreTransaction((SQLQueryEngine) getQueryEngine());
    }
}
