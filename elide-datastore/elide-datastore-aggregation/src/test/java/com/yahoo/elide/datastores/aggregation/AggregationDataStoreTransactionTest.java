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
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.pagination.PaginationImpl;
import com.yahoo.elide.datastores.aggregation.cache.Cache;
import com.yahoo.elide.datastores.aggregation.cache.QueryKeyExtractor;
import com.yahoo.elide.datastores.aggregation.core.QueryLogger;
import com.yahoo.elide.datastores.aggregation.core.QueryResponse;
import com.yahoo.elide.datastores.aggregation.example.PlayerStats;
import com.yahoo.elide.datastores.aggregation.framework.SQLUnitTest;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.datastores.aggregation.query.QueryResult;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.query.SQLQuery;
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
    @Mock private QueryLogger queryLogger;

    private Query query = Query.builder().table(playerStatsTable).build();
    private final String queryKey = QueryKeyExtractor.extractKey(query);
    private static final Iterable<Object> DATA = Collections.singletonList("xyzzy");

    // inject our own query instead of using buildQuery impl
    private class MyAggregationDataStoreTransaction extends AggregationDataStoreTransaction {

        public MyAggregationDataStoreTransaction(QueryEngine queryEngine, Cache cache, QueryLogger queryLogger) {
            super(queryEngine, cache, queryLogger);
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
        Mockito.reset(queryLogger);

        QueryResult queryResult = QueryResult.builder().data(DATA).build();
        SQLQuery myQuery = SQLQuery.builder().clientQuery(query)
                .fromClause(query.getTable().getName())
                .projectionClause(" ").build();
        when(queryEngine.getTableVersion(playerStatsTable, qeTransaction)).thenReturn("foo");
        when(queryEngine.executeQuery(query, qeTransaction)).thenReturn(queryResult);
        when(queryEngine.explain(query)).thenReturn(myQuery.toString());
        AggregationDataStoreTransaction transaction =
                new MyAggregationDataStoreTransaction(queryEngine, cache, queryLogger);
        EntityProjection entityProjection = EntityProjection.builder().type(PlayerStats.class).build();

        assertEquals(DATA, transaction.loadObjects(entityProjection, scope));

        String cacheKey = "foo;" + queryKey;
        Mockito.verify(cache).get(cacheKey);
        Mockito.verify(cache).put(cacheKey, queryResult);
        Mockito.verifyNoMoreInteractions(cache);
        Mockito.verify(queryLogger, times(1)).acceptQuery(
                Mockito.eq(scope.getRequestId()),
                any(), any(), any(), any(), any());
        Mockito.verify(queryLogger, times(1)).processQuery(
                Mockito.eq(scope.getRequestId()), any(), any(), Mockito.eq(false));
        Mockito.verify(queryLogger, times(1)).completeQuery(
                Mockito.eq(scope.getRequestId()), any());
    }

    @Test
    public void loadObjectsUsesCache() {
        Mockito.reset(queryLogger);

        String cacheKey = "foo;" + queryKey;
        QueryResult queryResult = QueryResult.builder().data(DATA).build();
        SQLQuery myQuery = SQLQuery.builder().clientQuery(query)
                .fromClause(query.getTable().getName())
                .projectionClause(" ").build();
        when(cache.get(cacheKey)).thenReturn(queryResult);
        when(queryEngine.getTableVersion(playerStatsTable, qeTransaction)).thenReturn("foo");
        when(queryEngine.explain(query)).thenReturn(myQuery.toString());
        AggregationDataStoreTransaction transaction =
                new MyAggregationDataStoreTransaction(queryEngine, cache, queryLogger);
        EntityProjection entityProjection = EntityProjection.builder().type(PlayerStats.class).build();

        assertEquals(DATA, transaction.loadObjects(entityProjection, scope));

        Mockito.verify(queryEngine, never()).executeQuery(any(), any());
        Mockito.verify(cache).get(cacheKey);
        Mockito.verifyNoMoreInteractions(cache);
        Mockito.verify(queryLogger, times(1)).acceptQuery(
                Mockito.eq(scope.getRequestId()),
                any(), any(), any(), any(), any());
        Mockito.verify(queryLogger, times(1)).processQuery(
                Mockito.eq(scope.getRequestId()), any(), any(), Mockito.eq(true));
        Mockito.verify(queryLogger, times(1)).completeQuery(
                Mockito.eq(scope.getRequestId()), any());
    }

    @Test
    public void loadObjectsPassesPagination() {
        Mockito.reset(queryLogger);

        QueryResult queryResult = QueryResult.builder().data(DATA).pageTotals(314L).build();
        SQLQuery myQuery = SQLQuery.builder().clientQuery(query)
                .fromClause(query.getTable().getName())
                .projectionClause(" ").build();
        when(cache.get(anyString())).thenReturn(queryResult);
        when(queryEngine.getTableVersion(playerStatsTable, qeTransaction)).thenReturn("foo");
        when(queryEngine.explain(query)).thenReturn(myQuery.toString());
        AggregationDataStoreTransaction transaction =
                new MyAggregationDataStoreTransaction(queryEngine, cache, queryLogger);
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
        Mockito.verify(queryLogger, times(1)).acceptQuery(
                Mockito.eq(scope.getRequestId()),
                any(), any(), any(), any(), any());
        Mockito.verify(queryLogger, times(1)).processQuery(
                Mockito.eq(scope.getRequestId()), any(), any(), Mockito.eq(true));
        Mockito.verify(queryLogger, times(1)).completeQuery(
                Mockito.eq(scope.getRequestId()), any());
    }

    @Test
    public void loadObjectsNoTableVersion() {
        Mockito.reset(queryLogger);

        SQLQuery myQuery = SQLQuery.builder().clientQuery(query)
                .fromClause(query.getTable().getName())
                .projectionClause(" ").build();
        when(queryEngine.executeQuery(query, qeTransaction))
                .thenReturn(QueryResult.builder().data(Collections.emptyList()).build());
        when(queryEngine.explain(query)).thenReturn(myQuery.toString());
        AggregationDataStoreTransaction transaction =
                new MyAggregationDataStoreTransaction(queryEngine, cache, queryLogger);
        EntityProjection entityProjection = EntityProjection.builder().type(PlayerStats.class).build();

        transaction.loadObjects(entityProjection, scope);

        Mockito.verifyNoInteractions(cache);
        Mockito.verify(queryLogger, times(1)).acceptQuery(
                Mockito.eq(scope.getRequestId()),
                any(), any(), any(), any(), any());
        Mockito.verify(queryLogger, times(1)).processQuery(
                Mockito.eq(scope.getRequestId()), any(), any(), Mockito.eq(false));
        Mockito.verify(queryLogger, times(1)).completeQuery(
                Mockito.eq(scope.getRequestId()), any());
    }

    @Test
    public void loadObjectsBypassCache() {
        Mockito.reset(queryLogger);

        query = Query.builder().table(playerStatsTable).bypassingCache(true).build();
        SQLQuery myQuery = SQLQuery.builder().clientQuery(query)
                .fromClause(query.getTable().getName())
                .projectionClause(" ").build();

        QueryResult queryResult = QueryResult.builder().data(DATA).build();
        when(queryEngine.executeQuery(query, qeTransaction)).thenReturn(queryResult);
        when(queryEngine.explain(query)).thenReturn(myQuery.toString());
        AggregationDataStoreTransaction transaction =
                new MyAggregationDataStoreTransaction(queryEngine, cache, queryLogger);
        EntityProjection entityProjection = EntityProjection.builder().type(PlayerStats.class).build();

        assertEquals(DATA, transaction.loadObjects(entityProjection, scope));

        Mockito.verify(queryEngine, never()).getTableVersion(any(), any());
        Mockito.verifyNoInteractions(cache);
        Mockito.verify(queryLogger, times(1)).acceptQuery(
                Mockito.eq(scope.getRequestId()),
                any(), any(), any(), any(), any());
        Mockito.verify(queryLogger, times(1)).processQuery(
                Mockito.eq(scope.getRequestId()), any(), any(), Mockito.eq(false));
        Mockito.verify(queryLogger, times(1)).completeQuery(
                Mockito.eq(scope.getRequestId()), any());
    }

    @Test
    public void loadObjectsExceptionThrownTest() throws Exception {
        Mockito.reset(queryLogger);
        String nullPointerExceptionMessage = "Cannot dereference an object with value Null";
        try {
            query = Query.builder().table(playerStatsTable).bypassingCache(true).build();
            doThrow(new NullPointerException(nullPointerExceptionMessage))
                    .when(queryEngine).executeQuery(query, qeTransaction);
            AggregationDataStoreTransaction transaction =
                    new MyAggregationDataStoreTransaction(queryEngine, cache, queryLogger);
            EntityProjection entityProjection = EntityProjection.builder().type(PlayerStats.class).build();
            transaction.loadObjects(entityProjection, scope);
        } catch (Exception e) {
                assertEquals(nullPointerExceptionMessage, e.getMessage());
                Mockito.verify(queryLogger).completeQuery(Mockito.eq(scope.getRequestId()),
                    argThat((QueryResponse qResponse) -> qResponse.getErrorMessage() == e.getMessage()));
        }

        Mockito.verify(queryLogger, times(1)).acceptQuery(
                Mockito.eq(scope.getRequestId()),
                any(), any(), any(), any(), any());
        Mockito.verify(queryLogger, times(1)).processQuery(
                Mockito.eq(scope.getRequestId()), any(), any(), Mockito.eq(false));
        Mockito.verify(queryLogger, times(1)).completeQuery(
                Mockito.eq(scope.getRequestId()), any());
    }

    @Test
    public void aggregationQueryLoggerCancelQueryTest() {
        Mockito.reset(queryLogger);
        AggregationDataStoreTransaction transaction =
                new MyAggregationDataStoreTransaction(queryEngine, cache, queryLogger);
        transaction.cancel(scope);
        Mockito.verify(queryLogger, times(1)).cancelQuery(Mockito.eq(scope.getRequestId()));
    }
}
