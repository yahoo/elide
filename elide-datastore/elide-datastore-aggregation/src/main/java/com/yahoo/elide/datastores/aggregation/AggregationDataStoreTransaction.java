/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation;

import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.QueryLogger;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.datastores.aggregation.cache.Cache;
import com.yahoo.elide.datastores.aggregation.cache.QueryKeyExtractor;
import com.yahoo.elide.datastores.aggregation.metadata.models.Table;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.datastores.aggregation.query.QueryResult;
import com.yahoo.elide.request.EntityProjection;

import com.google.common.annotations.VisibleForTesting;

import lombok.ToString;

import java.io.IOException;
import java.security.Principal;

/**
 * Transaction handler for {@link AggregationDataStore}.
 */
@ToString
public class AggregationDataStoreTransaction implements DataStoreTransaction {
    private final QueryEngine queryEngine;
    private final Cache cache;
    private final QueryEngine.Transaction queryEngineTransaction;
    private final QueryLogger queryLogger;

    public AggregationDataStoreTransaction(QueryEngine queryEngine, Cache cache,
                                           QueryLogger queryLogger) {
        this.queryEngine = queryEngine;
        this.cache = cache;
        this.queryEngineTransaction = queryEngine.beginTransaction();
        this.queryLogger = queryLogger;
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
        queryEngineTransaction.close();
    }

    @Override
    public void createObject(Object entity, RequestScope scope) {

    }

    @Override
    public Iterable<Object> loadObjects(EntityProjection entityProjection, RequestScope scope) {
        Query query = buildQuery(entityProjection, scope);
        QueryResult result = null;

        String cacheKey = null;
        if (cache != null && !query.isBypassingCache()) {
            String tableVersion = queryEngine.getTableVersion(query.getTable(), queryEngineTransaction);
            if (tableVersion != null) {
                cacheKey = tableVersion + ';' + QueryKeyExtractor.extractKey(query);
                result = cache.get(cacheKey);
            }
        }
        if (result == null) {
            result = queryEngine.executeQuery(query, queryEngineTransaction);
            queryEngine.explainQuery(query, queryLogger, scope,
                    entityProjection.getType().getName());
            if (cacheKey != null) {
                cache.put(cacheKey, result);
            }
        }
        if (entityProjection.getPagination() != null && entityProjection.getPagination().returnPageTotals()) {
            entityProjection.getPagination().setPageTotals(result.getPageTotals());
        }
        //ElideResponse = null (for now)
        queryLogger.completeQuery(scope.getRequestId(), null);
        return result.getData();
    }

    @Override
    public void close() throws IOException {
        queryEngineTransaction.close();
    }

    @VisibleForTesting
    Query buildQuery(EntityProjection entityProjection, RequestScope scope) {
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
        queryEngineTransaction.cancel();
    }
}
