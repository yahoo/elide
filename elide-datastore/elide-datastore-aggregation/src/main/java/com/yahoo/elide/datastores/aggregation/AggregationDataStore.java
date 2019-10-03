/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation;

import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.datastores.aggregation.schema.Schema;
import com.yahoo.elide.request.EntityProjection;

/**
 * DataStore that supports Aggregation. Uses {@link QueryEngine} to return results.
 */
public abstract class AggregationDataStore implements DataStore {

    private QueryEngine queryEngine;

    public AggregationDataStore(QueryEngine queryEngine) {
        this.queryEngine = queryEngine;
    }

    @Override
    public abstract void populateEntityDictionary(EntityDictionary dictionary);

    @Override
    public DataStoreTransaction beginTransaction() {
        return new AggregationDataStoreTransaction(queryEngine);
    }

    public static Query buildQuery(EntityProjection entityProjection, RequestScope scope) {
        Schema schema = new Schema(entityProjection.getType(), scope.getDictionary());
        AggregationDataStoreHelper agHelper = new AggregationDataStoreHelper(schema, scope);
        return agHelper.getQuery();
    }
}
