/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation;

import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.datastores.aggregation.schema.Schema;
import com.yahoo.elide.request.EntityProjection;

import com.google.common.annotations.VisibleForTesting;

import java.io.IOException;

/**
 * Transaction handler for {@link AggregationDataStore}.
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
        Query query = buildQuery(entityProjection, scope);
        return queryEngine.executeQuery(query);
    }

    @Override
    public void close() throws IOException {

    }

    @VisibleForTesting
    Query buildQuery(EntityProjection entityProjection, RequestScope scope) {
        Schema schema = queryEngine.getSchema(entityProjection.getType());

        AggregationDataStoreHelper agHelper = new AggregationDataStoreHelper(schema, entityProjection);
        return agHelper.getQuery();
    }
}
