/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation;

import com.yahoo.elide.core.DataStoreTransactionImplementation;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.datastores.aggregation.metadata.models.Table;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.datastores.aggregation.query.QueryResult;
import com.yahoo.elide.request.EntityProjection;

import com.google.common.annotations.VisibleForTesting;

import java.io.IOException;
import java.util.concurrent.Future;
import javax.persistence.EntityManager;

/**
 * Transaction handler for {@link AggregationDataStore}.
 */
public class AggregationDataStoreTransaction extends DataStoreTransactionImplementation {
    private EntityManager em;
    private QueryEngine queryEngine;
    private Future<QueryResult> queryResult;
    public AggregationDataStoreTransaction(EntityManager em, QueryEngine queryEngine) {
        this.em = em;
	this.queryEngine = queryEngine;
	this.aggregationDataStoreTransactionCancel = aggregationDataStoreTransactionCancel;
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
        queryResult = queryEngine.executeQuery(query);
	queryResult.run();
	QueryResult result = queryResult.get();
        if (entityProjection.getPagination() != null && entityProjection.getPagination().returnPageTotals()) {
            entityProjection.getPagination().setPageTotals(result.getPageTotals());
        }
        return result.getData();
    }

    @Override
    public void close() throws IOException {

    }

    @VisibleForTesting
    private Query buildQuery(EntityProjection entityProjection, RequestScope scope) {
        Table table = queryEngine.getTable(scope.getDictionary().getJsonAliasFor(entityProjection.getType()));
        EntityProjectionTranslator translator = new EntityProjectionTranslator(
                queryEngine,
                table,
                entityProjection,
                scope.getDictionary());
        return translator.getQuery();
    }

    @Override
    public void cancel() {
        result.cancel(true); 
    }
}
