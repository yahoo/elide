/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation;

import static com.yahoo.elide.request.Pagination.DEFAULT_PAGE_LIMIT;
import static com.yahoo.elide.request.Pagination.MAX_PAGE_LIMIT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.pagination.PaginationImpl;
import com.yahoo.elide.datastores.aggregation.example.PlayerStats;
import com.yahoo.elide.datastores.aggregation.framework.SQLUnitTest;
import com.yahoo.elide.datastores.aggregation.query.Cache;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.datastores.aggregation.query.QueryKeyExtractor;
import com.yahoo.elide.datastores.aggregation.query.QueryResult;
import com.yahoo.elide.request.EntityProjection;
import com.yahoo.elide.request.Pagination;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

@ExtendWith(MockitoExtension.class)
class AggregationDataStoreTransactionTest extends SQLUnitTest {

    @Mock private QueryEngine queryEngine;
    @Mock private QueryEngine.Transaction qeTransaction;
    @Mock private RequestScope scope;
    @Mock private Cache cache;

    private Query query = Query.builder().table(playerStatsTable).build();
    private final String queryKey = QueryKeyExtractor.extractKey(query);
    private static final Iterable<Object> DATA = Collections.singletonList("xyzzy");

    // inject our own query instead of using buildQuery impl
    private class MyAggregationDataStoreTransaction extends AggregationDataStoreTransaction {

        public MyAggregationDataStoreTransaction(QueryEngine queryEngine, Cache cache) {
            super(queryEngine, cache);
        }

        @Override
        protected Query buildQuery(EntityProjection entityProjection, RequestScope scope) {
            return query;
        }
    }

    @BeforeAll
    public static void beforeAllTests() {
        SQLUnitTest.init();
    }

    @BeforeEach
    public void setUp() {
        when(queryEngine.beginTransaction()).thenReturn(qeTransaction);
    }

    @Test
    public void loadObjectsPopulatesCache() {
        QueryResult queryResult = QueryResult.builder().data(DATA).build();
        when(queryEngine.getTableVersion(playerStatsTable, qeTransaction)).thenReturn("foo");
        when(queryEngine.executeQuery(query, qeTransaction)).thenReturn(queryResult);
        AggregationDataStoreTransaction transaction = new MyAggregationDataStoreTransaction(queryEngine, cache);
        EntityProjection entityProjection = EntityProjection.builder().type(PlayerStats.class).build();

        assertEquals(DATA, transaction.loadObjects(entityProjection, scope));

        String cacheKey = "foo;" + queryKey;
        Mockito.verify(cache).get(cacheKey);
        Mockito.verify(cache).put(cacheKey, queryResult);
        Mockito.verifyNoMoreInteractions(cache);
    }

    @Test
    public void loadObjectsUsesCache() {
        String cacheKey = "foo;" + queryKey;
        QueryResult queryResult = QueryResult.builder().data(DATA).build();
        when(cache.get(cacheKey)).thenReturn(queryResult);
        when(queryEngine.getTableVersion(playerStatsTable, qeTransaction)).thenReturn("foo");
        AggregationDataStoreTransaction transaction = new MyAggregationDataStoreTransaction(queryEngine, cache);
        EntityProjection entityProjection = EntityProjection.builder().type(PlayerStats.class).build();

        assertEquals(DATA, transaction.loadObjects(entityProjection, scope));

        Mockito.verify(queryEngine, never()).executeQuery(any(), any());
        Mockito.verify(cache).get(cacheKey);
        Mockito.verifyNoMoreInteractions(cache);
    }

    @Test
    public void loadObjectsPassesPagination() {
        QueryResult queryResult = QueryResult.builder().data(DATA).pageTotals(314L).build();
        when(cache.get(anyString())).thenReturn(queryResult);
        when(queryEngine.getTableVersion(playerStatsTable, qeTransaction)).thenReturn("foo");
        AggregationDataStoreTransaction transaction = new MyAggregationDataStoreTransaction(queryEngine, cache);
        Pagination pagination = new PaginationImpl(
                String.class, null, null, DEFAULT_PAGE_LIMIT, MAX_PAGE_LIMIT, true, false);
        EntityProjection entityProjection = EntityProjection.builder()
                .type(PlayerStats.class).pagination(pagination).build();

        assertEquals(DATA, transaction.loadObjects(entityProjection, scope));
        assertEquals(314L, entityProjection.getPagination().getPageTotals());

        String cacheKey = "foo;" + queryKey;
        Mockito.verify(queryEngine, never()).executeQuery(any(), any());
        Mockito.verify(cache).get(cacheKey);
        Mockito.verifyNoMoreInteractions(cache);
    }

    @Test
    public void loadObjectsNoTableVersion() {
        when(queryEngine.executeQuery(query, qeTransaction))
                .thenReturn(QueryResult.builder().data(Collections.emptyList()).build());
        AggregationDataStoreTransaction transaction = new MyAggregationDataStoreTransaction(queryEngine, cache);
        EntityProjection entityProjection = EntityProjection.builder().type(PlayerStats.class).build();

        transaction.loadObjects(entityProjection, scope);

        Mockito.verifyNoInteractions(cache);
    }

    @Test
    public void loadObjectsBypassCache() {
        query = Query.builder().table(playerStatsTable).bypassingCache(true).build();

        QueryResult queryResult = QueryResult.builder().data(DATA).build();
        when(queryEngine.executeQuery(query, qeTransaction)).thenReturn(queryResult);
        AggregationDataStoreTransaction transaction = new MyAggregationDataStoreTransaction(queryEngine, cache);
        EntityProjection entityProjection = EntityProjection.builder().type(PlayerStats.class).build();

        assertEquals(DATA, transaction.loadObjects(entityProjection, scope));

        Mockito.verify(queryEngine, never()).getTableVersion(any(), any());
        Mockito.verifyNoInteractions(cache);
    }
}
