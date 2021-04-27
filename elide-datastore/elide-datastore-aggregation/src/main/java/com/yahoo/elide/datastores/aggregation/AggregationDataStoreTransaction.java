/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation;

import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.exceptions.BadRequestException;
import com.yahoo.elide.core.exceptions.HttpStatus;
import com.yahoo.elide.core.exceptions.HttpStatusException;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.request.EntityProjection;
import com.yahoo.elide.datastores.aggregation.cache.Cache;
import com.yahoo.elide.datastores.aggregation.cache.QueryKeyExtractor;
import com.yahoo.elide.datastores.aggregation.core.QueryLogger;
import com.yahoo.elide.datastores.aggregation.core.QueryResponse;
import com.yahoo.elide.datastores.aggregation.filter.visitor.MatchesTemplateVisitor;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import com.yahoo.elide.datastores.aggregation.metadata.models.Table;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.datastores.aggregation.query.QueryResult;
import com.google.common.annotations.VisibleForTesting;
import lombok.ToString;

import java.io.IOException;
import java.util.List;

/**
 * Transaction handler for {@link AggregationDataStore}.
 */
@ToString
public class AggregationDataStoreTransaction implements DataStoreTransaction {
    private final QueryEngine queryEngine;
    private final Cache cache;
    private final QueryEngine.Transaction queryEngineTransaction;
    private final QueryLogger queryLogger;
    private final MetaDataStore metaDataStore;

    public AggregationDataStoreTransaction(QueryEngine queryEngine, Cache cache,
                                           QueryLogger queryLogger) {
        this.queryEngine = queryEngine;
        this.cache = cache;
        this.queryEngineTransaction = queryEngine.beginTransaction();
        this.queryLogger = queryLogger;
        this.metaDataStore = queryEngine.getMetaDataStore();
    }

    @Override
    public <T> void save(T entity, RequestScope scope) {

    }

    @Override
    public <T> void delete(T entity, RequestScope scope) {

    }

    @Override
    public void flush(RequestScope scope) {

    }

    @Override
    public void commit(RequestScope scope) {
        queryEngineTransaction.close();
    }

    @Override
    public <T> void createObject(T entity, RequestScope scope) {

    }

    @Override
    public <T> Iterable<T> loadObjects(EntityProjection entityProjection, RequestScope scope) {
        QueryResult result = null;
        QueryResponse response = null;
        String cacheKey = null;
        try {
            queryLogger.acceptQuery(scope.getRequestId(), scope.getUser(), scope.getHeaders(),
                    scope.getApiVersion(), scope.getQueryParams(), scope.getPath());
            Query query = buildQuery(entityProjection, scope);
            Table table = (Table) query.getSource();
            if (cache != null && !query.isBypassingCache()) {
                String tableVersion = queryEngine.getTableVersion(table, queryEngineTransaction);
                tableVersion = tableVersion == null ? "" : tableVersion;

                cacheKey = tableVersion + ';' + QueryKeyExtractor.extractKey(query);
                result = cache.get(cacheKey);
            }

            boolean isCached = result != null;
            List<String> queryText = queryEngine.explain(query);
            queryLogger.processQuery(scope.getRequestId(), query, queryText, isCached);
            if (result == null) {
                result = queryEngine.executeQuery(query, queryEngineTransaction);
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

    @Override
    public void close() throws IOException {
        queryEngineTransaction.close();
    }

    @VisibleForTesting
    Query buildQuery(EntityProjection entityProjection, RequestScope scope) {
        Table table = metaDataStore.getTable(
                scope.getDictionary().getJsonAliasFor(entityProjection.getType()),
                scope.getApiVersion());
        String bypassCacheStr = scope.getRequestHeaderByName("bypasscache");
        Boolean bypassCache = "true".equals(bypassCacheStr);
        EntityProjectionTranslator translator = new EntityProjectionTranslator(
                queryEngine,
                table,
                entityProjection,
                scope,
                bypassCache);

        Query query = translator.getQuery();

        FilterExpression filterTemplate = table.getRequiredFilter(scope.getDictionary());
        if (filterTemplate != null && ! MatchesTemplateVisitor.isValid(filterTemplate, query.getWhereFilter())) {
            String message = String.format("Querying %s requires a mandatory filter: %s",
                        table.getName(), table.getRequiredFilter());

            throw new BadRequestException(message);
        }

        return query;
    }

    @Override
    public void cancel(RequestScope scope) {
        queryLogger.cancelQuery(scope.getRequestId());
        queryEngineTransaction.cancel();
    }
}
