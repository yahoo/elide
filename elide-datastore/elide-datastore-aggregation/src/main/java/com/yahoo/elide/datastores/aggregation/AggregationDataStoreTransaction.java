/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation;

import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.request.EntityProjection;

import java.io.IOException;

/**
 * Transaction handler for {@link AggregationDataStore}
 */
public class AggregationDataStoreTransaction implements DataStoreTransaction {
    private QueryEngine queryEngine;

    public AggregationDataStoreTransaction(QueryEngine queryEngine) {
        this.queryEngine = queryEngine;
    }

    @Override
    public void save(Object entity, RequestScope scope) {

    }

    @Override
    public void delete(Object entity, RequestScope scope) {

    }

    @Override
    public void flush(RequestScope scope) {

    }

    @Override
    public void commit(RequestScope scope) {

    }

    @Override
    public void createObject(Object entity, RequestScope scope) {

    }

    @Override
    public Iterable<Object> loadObjects(EntityProjection entityProjection, RequestScope scope) {
        Query query = AggregationDataStore.buildQuery(entityProjection, scope);
        return queryEngine.executeQuery(query);
    }

    @Override
    public void close() throws IOException {

    }
}
