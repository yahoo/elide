/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation;

import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.datastore.DataStoreIterable;
import com.yahoo.elide.core.datastore.DataStoreIterableBuilder;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.exceptions.BadRequestException;
import com.yahoo.elide.core.exceptions.HttpStatus;
import com.yahoo.elide.core.exceptions.HttpStatusException;
import com.yahoo.elide.core.exceptions.InvalidOperationException;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.request.Argument;
import com.yahoo.elide.core.request.EntityProjection;
import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.datastores.aggregation.cache.Cache;
import com.yahoo.elide.datastores.aggregation.cache.QueryKeyExtractor;
import com.yahoo.elide.datastores.aggregation.core.QueryLogger;
import com.yahoo.elide.datastores.aggregation.core.QueryResponse;
import com.yahoo.elide.datastores.aggregation.filter.visitor.MatchesTemplateVisitor;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import com.yahoo.elide.datastores.aggregation.metadata.models.Column;
import com.yahoo.elide.datastores.aggregation.metadata.models.RequiresFilter;
import com.yahoo.elide.datastores.aggregation.metadata.models.Table;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.datastores.aggregation.query.QueryResult;
import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.compress.utils.Lists;

import lombok.ToString;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        throwReadOnlyException(entity);
    }

    @Override
    public <T> void delete(T entity, RequestScope scope) {
        throwReadOnlyException(entity);
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
        throwReadOnlyException(entity);
    }

    @Override
    public <T> DataStoreIterable<T> loadObjects(EntityProjection entityProjection, RequestScope scope) {
        QueryResult result = null;
        QueryResponse response = null;
        String cacheKey = null;
        try {

            //Convert multivalued map to map.
            Map<String, String> headers = scope.getRequestHeaders().entrySet()
                    .stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            (entry) -> entry.getValue().stream().collect(Collectors.joining(" "))
                    ));

            queryLogger.acceptQuery(scope.getRequestId(), scope.getUser(), headers,
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

                    //The query result needs to be streamed into an in memory list before caching.
                    //TODO - add a cap to how many records can be streamed back.  If this is exceeded, abort caching
                    //and return the results.
                    QueryResult cacheableResult = QueryResult.builder()
                            .data(Lists.newArrayList(result.getData().iterator()))
                            .pageTotals(result.getPageTotals())
                            .build();
                    cache.put(cacheKey, cacheableResult);
                    result = cacheableResult;
                }
            }
            if (entityProjection.getPagination() != null && entityProjection.getPagination().returnPageTotals()) {
                entityProjection.getPagination().setPageTotals(result.getPageTotals());
            }
            response = new QueryResponse(HttpStatus.SC_OK, result.getData(), null);
            return new DataStoreIterableBuilder(result.getData()).build();
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

        Query modifiedQuery = addTableFilterArguments(table, query, scope.getDictionary());
        modifiedQuery = addColumnFilterArguments(table, modifiedQuery, scope.getDictionary());

        return modifiedQuery;
    }

    @VisibleForTesting
    Query addTableFilterArguments(Table table, Query query, EntityDictionary dictionary) {
        FilterExpression filterTemplate = table.getRequiredFilter(dictionary);

        Query modifiedQuery = query;
        if (filterTemplate != null) {
            Map<String, Argument> allArguments = validateRequiredFilter(filterTemplate, query, table);

            if (!allArguments.isEmpty()) {
                if (query.getArguments() != null) {
                    allArguments.putAll(query.getArguments());
                }

                modifiedQuery = Query.builder()
                        .query(query)
                        .arguments(allArguments)
                        .build();
            }
        }

        return modifiedQuery;
    }

    @VisibleForTesting
    Query addColumnFilterArguments(Table table, Query query, EntityDictionary dictionary) {

        Query.QueryBuilder queryBuilder = Query.builder();

        query.getColumnProjections().stream().forEach(projection -> {
            Column column = table.getColumn(Column.class, projection.getName());

            FilterExpression requiredFilter = column.getRequiredFilter(dictionary);

            if (requiredFilter != null) {
                Map<String, Argument> allArguments = validateRequiredFilter(requiredFilter, query, column);
                if (projection.getArguments() != null) {
                    allArguments.putAll(projection.getArguments());
                }
                queryBuilder.column(projection.withArguments(allArguments));
            } else {
                queryBuilder.column(projection);
            }
        });

        return queryBuilder
                .arguments(query.getArguments())
                .havingFilter(query.getHavingFilter())
                .whereFilter(query.getWhereFilter())
                .sorting(query.getSorting())
                .pagination(query.getPagination())
                .bypassingCache(query.isBypassingCache())
                .source(query.getSource())
                .scope(query.getScope())
                .build();
    }

    private Map<String, Argument> validateRequiredFilter(
            FilterExpression filterTemplate,
            Query query,
            RequiresFilter requiresFilter
    ) {
        Map<String, Argument> templateFilterArguments = new HashMap<>();
        if (!MatchesTemplateVisitor.isValid(filterTemplate, query.getWhereFilter(), templateFilterArguments)
                && (!MatchesTemplateVisitor.isValid(filterTemplate, query.getHavingFilter(),
                templateFilterArguments))) {
            String message = String.format("Querying %s requires a mandatory filter: %s",
                    requiresFilter.getName(), requiresFilter.getRequiredFilter());

            throw new BadRequestException(message);
        }

        return templateFilterArguments;
    }

    @Override
    public void cancel(RequestScope scope) {
        queryLogger.cancelQuery(scope.getRequestId());
        queryEngineTransaction.cancel();
    }

    private <T> void throwReadOnlyException(T entity) {
        EntityDictionary dictionary = metaDataStore.getMetadataDictionary();
        Type<?> type  = dictionary.getType(entity);
        throw new InvalidOperationException(dictionary.getJsonAliasFor(type) + " is read only.");
    }
}
