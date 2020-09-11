/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.transactions;

import com.yahoo.elide.core.HttpStatus;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.exceptions.HttpStatusException;
import com.yahoo.elide.datastores.aggregation.AggregationDataStore;
import com.yahoo.elide.datastores.aggregation.AggregationDataStoreTransaction;
import com.yahoo.elide.datastores.aggregation.QueryEngine;
import com.yahoo.elide.datastores.aggregation.cache.Cache;
import com.yahoo.elide.datastores.aggregation.cache.QueryKeyExtractor;
import com.yahoo.elide.datastores.aggregation.core.QueryLogger;
import com.yahoo.elide.datastores.aggregation.core.QueryResponse;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.datastores.aggregation.query.QueryResult;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.SQLQueryEngine;
import com.yahoo.elide.request.EntityProjection;

import lombok.ToString;

import java.util.List;

/**
 * SQL Transaction handler for {@link AggregationDataStore}.
 */
@ToString
public class SQLAggregationDataStoreTransaction extends AggregationDataStoreTransaction {

    public SQLAggregationDataStoreTransaction(QueryEngine queryEngine, Cache cache, QueryLogger queryLogger) {
        super(queryEngine, cache, queryLogger);
    }

    @Override
    public Iterable<Object> loadObjects(EntityProjection entityProjection, RequestScope scope) {

        SQLQueryEngine sqlQueryEngine = (SQLQueryEngine) queryEngine;
        QueryResult result = null;
        QueryResponse response = null;
        String cacheKey = null;
        try {
            queryLogger.acceptQuery(scope.getRequestId(), scope.getUser(), scope.getHeaders(),
                    scope.getApiVersion(), scope.getQueryParams(), scope.getPath());
            Query query = buildQuery(entityProjection, scope);
            String connectionName = query.getTable().getDbConnectionName();
            List<String> queryText;
            if (connectionName == null || connectionName.trim().isEmpty()) {
                this.queryEngineTransaction = sqlQueryEngine.beginTransaction();
                queryText = sqlQueryEngine.explain(query);
            } else {
                this.queryEngineTransaction = sqlQueryEngine.beginTransaction(connectionName);
                queryText = sqlQueryEngine.explain(query, connectionName);
            }
            if (cache != null && !query.isBypassingCache()) {
                String tableVersion = sqlQueryEngine.getTableVersion(query.getTable(), queryEngineTransaction);
                if (tableVersion != null) {
                    cacheKey = tableVersion + ';' + QueryKeyExtractor.extractKey(query);
                    result = cache.get(cacheKey);
                }
            }
            boolean isCached = result == null ? false : true;
            queryLogger.processQuery(scope.getRequestId(), query, queryText, isCached);
            if (result == null) {
                result = sqlQueryEngine.executeQuery(query, queryEngineTransaction);
                if (cacheKey != null) {
                    cache.put(cacheKey, result);
                }
            }
            if (entityProjection.getPagination() != null && entityProjection.getPagination().returnPageTotals()) {
                entityProjection.getPagination().setPageTotals(result.getPageTotals());
            }
            response = new QueryResponse(HttpStatus.SC_OK, result.getData(), null);
            return result.getData();
        } catch (HttpStatusException e) {
            response = new QueryResponse(e.getStatus(), null, e.getMessage());
            throw e;
        } catch (Exception e) {
            response = new QueryResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, null, e.getMessage());
            throw e;
        } finally {
            queryLogger.completeQuery(scope.getRequestId(), response);
        }
    }
}
