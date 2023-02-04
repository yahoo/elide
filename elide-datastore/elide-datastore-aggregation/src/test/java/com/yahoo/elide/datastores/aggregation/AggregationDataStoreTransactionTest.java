/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation;

import static com.yahoo.elide.core.dictionary.EntityDictionary.NO_VERSION;
import static com.yahoo.elide.core.request.Pagination.DEFAULT_PAGE_LIMIT;
import static com.yahoo.elide.core.request.Pagination.MAX_PAGE_LIMIT;
import static com.yahoo.elide.datastores.aggregation.dynamic.NamespacePackage.DEFAULT_NAMESPACE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.exceptions.BadRequestException;
import com.yahoo.elide.core.filter.dialect.RSQLFilterDialect;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.pagination.PaginationImpl;
import com.yahoo.elide.core.request.Argument;
import com.yahoo.elide.core.request.EntityProjection;
import com.yahoo.elide.core.request.Pagination;
import com.yahoo.elide.core.type.ClassType;
import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.core.utils.DefaultClassScanner;
import com.yahoo.elide.core.utils.coerce.CoerceUtil;
import com.yahoo.elide.core.utils.coerce.converters.ISO8601DateSerde;
import com.yahoo.elide.datastores.aggregation.cache.Cache;
import com.yahoo.elide.datastores.aggregation.cache.QueryKeyExtractor;
import com.yahoo.elide.datastores.aggregation.core.QueryLogger;
import com.yahoo.elide.datastores.aggregation.core.QueryResponse;
import com.yahoo.elide.datastores.aggregation.framework.SQLUnitTest;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import com.yahoo.elide.datastores.aggregation.metadata.models.Namespace;
import com.yahoo.elide.datastores.aggregation.metadata.models.Table;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.datastores.aggregation.query.QueryResult;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.SQLQueryEngine;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLTable;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.query.NativeQuery;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.query.SQLMetricProjection;
import com.google.common.collect.Lists;
import example.PlayerStats;
import example.PlayerStatsWithRequiredFilter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
class AggregationDataStoreTransactionTest extends SQLUnitTest {

    @Mock private SQLQueryEngine queryEngine;
    @Mock private QueryEngine.Transaction qeTransaction;
    @Mock private RequestScope scope;
    @Mock private Cache cache;
    @Mock private QueryLogger queryLogger;

    private Query query = Query.builder().source(playerStatsTable).build();
    private final String queryKey = QueryKeyExtractor.extractKey(query);
    private static final Iterable<Object> DATA = Collections.singletonList("xyzzy");

    // inject our own query instead of using buildQuery impl
    private class MyAggregationDataStoreTransaction extends AggregationDataStoreTransaction {
        public MyAggregationDataStoreTransaction(QueryEngine queryEngine, Cache cache, QueryLogger queryLogger) {
            super(queryEngine, cache, queryLogger);
        }

        @Override
        Query buildQuery(EntityProjection entityProjection, RequestScope scope) {
            return query;
        }
    }

    public AggregationDataStoreTransactionTest() {
        metaDataStore = new MetaDataStore(DefaultClassScanner.getInstance(), false);
        CoerceUtil.register(Date.class, new ISO8601DateSerde());
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
    public void testMissingRequiredTableFilter() {
        EntityDictionary dictionary = EntityDictionary.builder()
                .build();
        dictionary.bindEntity(PlayerStatsWithRequiredFilter.class);

        Table table = new SQLTable(new Namespace(DEFAULT_NAMESPACE),
                ClassType.of(PlayerStatsWithRequiredFilter.class), dictionary);

        AggregationDataStoreTransaction tx = new AggregationDataStoreTransaction(queryEngine, cache, queryLogger);
        assertThrows(BadRequestException.class, () -> tx.addTableFilterArguments(table, query, dictionary));
    }

    @Test
    public void testRequiredTableFilterArguments() throws Exception {
        Type<PlayerStatsWithRequiredFilter> tableType = ClassType.of(PlayerStatsWithRequiredFilter.class);

        EntityDictionary dictionary = EntityDictionary.builder()
                .build();
        dictionary.bindEntity(PlayerStatsWithRequiredFilter.class);

        SQLTable table = new SQLTable(new Namespace(DEFAULT_NAMESPACE), tableType, dictionary);

        RSQLFilterDialect filterDialect = RSQLFilterDialect.builder().dictionary(dictionary).build();
        FilterExpression where = filterDialect.parse(tableType, new HashSet<>(),
                "recordedDate>=2019-07-12T00:00Z;recordedDate<2030-07-12T00:00Z", NO_VERSION);

        Query query = Query.builder()
                .source(table)
                .whereFilter(where).build();

        AggregationDataStoreTransaction tx = new AggregationDataStoreTransaction(queryEngine, cache, queryLogger);

        Query modifiedQuery = tx.addTableFilterArguments(table, query, dictionary);
        Map<String, Argument> tableArguments = modifiedQuery.getArguments();
        assertTrue(tableArguments.containsKey("start"));
        assertTrue(tableArguments.containsKey("end"));
        assertEquals(2, tableArguments.size());
    }

    @Test
    public void testMissingRequiredColumnFilter() throws Exception {
        Type<PlayerStatsWithRequiredFilter> tableType = ClassType.of(PlayerStatsWithRequiredFilter.class);

        EntityDictionary dictionary = EntityDictionary.builder()
                .build();
        dictionary.bindEntity(PlayerStatsWithRequiredFilter.class);

        SQLTable table = new SQLTable(new Namespace(DEFAULT_NAMESPACE), tableType, dictionary);

        Query query = Query.builder()
                .column(SQLMetricProjection.builder().name("highScore").build())
                .source(table)
                .build();

        AggregationDataStoreTransaction tx = new AggregationDataStoreTransaction(queryEngine, cache, queryLogger);

        assertThrows(BadRequestException.class, () -> tx.addColumnFilterArguments(table, query, dictionary));
    }

    @Test
    public void testRequiredColumnFilterArguments() throws Exception {
        Type<PlayerStatsWithRequiredFilter> tableType = ClassType.of(PlayerStatsWithRequiredFilter.class);

        EntityDictionary dictionary = EntityDictionary.builder()
                .build();
        dictionary.bindEntity(PlayerStatsWithRequiredFilter.class);

        SQLTable table = new SQLTable(new Namespace(DEFAULT_NAMESPACE), tableType, dictionary);

        RSQLFilterDialect filterDialect = RSQLFilterDialect.builder().dictionary(dictionary).build();
        FilterExpression where = filterDialect.parse(tableType, new HashSet<>(),
                "recordedDate>2019-07-12T00:00Z", NO_VERSION);

        Query query = Query.builder()
                .column(SQLMetricProjection.builder().name("highScore").alias("highScore").build())
                .whereFilter(where)
                .source(table)
                .build();

        AggregationDataStoreTransaction tx = new AggregationDataStoreTransaction(queryEngine, cache, queryLogger);

        Query modifiedQuery = tx.addColumnFilterArguments(table, query, dictionary);
        Map<String, Argument> columnArguments = modifiedQuery.getColumnProjection("highScore").getArguments();
        assertTrue(columnArguments.containsKey("recordedDate"));
        assertEquals(1, columnArguments.size());
    }

    @Test
    public void loadObjectsPopulatesCache() {
        Mockito.reset(queryLogger);

        QueryResult queryResult = QueryResult.builder().data(DATA).build();
        NativeQuery myQuery = NativeQuery.builder()
                .fromClause(playerStatsTable.getName())
                .projectionClause(" ").build();
        when(queryEngine.getTableVersion(playerStatsTable, qeTransaction)).thenReturn("foo");
        when(queryEngine.executeQuery(query, qeTransaction)).thenReturn(queryResult);
        when(queryEngine.explain(query)).thenReturn(Arrays.asList(myQuery.toString()));
        AggregationDataStoreTransaction transaction =
                new MyAggregationDataStoreTransaction(queryEngine, cache, queryLogger);
        EntityProjection entityProjection = EntityProjection.builder().type(PlayerStats.class).build();

        assertEquals(DATA, Lists.newArrayList(transaction.loadObjects(entityProjection, scope)));

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
        NativeQuery myQuery = NativeQuery.builder()
                .fromClause(playerStatsTable.getName())
                .projectionClause(" ").build();
        when(cache.get(cacheKey)).thenReturn(queryResult);
        when(queryEngine.getTableVersion(playerStatsTable, qeTransaction)).thenReturn("foo");
        when(queryEngine.explain(query)).thenReturn(Arrays.asList(myQuery.toString()));
        AggregationDataStoreTransaction transaction =
                new MyAggregationDataStoreTransaction(queryEngine, cache, queryLogger);
        EntityProjection entityProjection = EntityProjection.builder().type(PlayerStats.class).build();

        assertEquals(DATA, Lists.newArrayList(transaction.loadObjects(entityProjection, scope)));

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
        NativeQuery myQuery = NativeQuery.builder()
                .fromClause(playerStatsTable.getName())
                .projectionClause(" ").build();
        when(cache.get(anyString())).thenReturn(queryResult);
        when(queryEngine.getTableVersion(playerStatsTable, qeTransaction)).thenReturn("foo");
        when(queryEngine.explain(query)).thenReturn(Arrays.asList(myQuery.toString()));
        AggregationDataStoreTransaction transaction =
                new MyAggregationDataStoreTransaction(queryEngine, cache, queryLogger);
        Pagination pagination = new PaginationImpl(
                String.class, null, null, DEFAULT_PAGE_LIMIT, MAX_PAGE_LIMIT, true, false);
        EntityProjection entityProjection = EntityProjection.builder()
                .type(PlayerStats.class).pagination(pagination).build();

        assertEquals(DATA, Lists.newArrayList(transaction.loadObjects(entityProjection, scope)));
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

        NativeQuery myQuery = NativeQuery.builder()
                .fromClause(playerStatsTable.getName())
                .projectionClause(" ").build();

        QueryResult queryResult = QueryResult.builder().data(DATA).build();

        when(queryEngine.executeQuery(query, qeTransaction))
                .thenReturn(queryResult);
        when(queryEngine.explain(query)).thenReturn(Arrays.asList(myQuery.toString()));
        AggregationDataStoreTransaction transaction =
                new MyAggregationDataStoreTransaction(queryEngine, cache, queryLogger);
        EntityProjection entityProjection = EntityProjection.builder().type(PlayerStats.class).build();

        transaction.loadObjects(entityProjection, scope);

        String cacheKey = ";" + queryKey;
        Mockito.verify(cache).get(cacheKey);
        Mockito.verify(cache).put(cacheKey, queryResult);
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

        query = Query.builder().source(playerStatsTable).bypassingCache(true).build();
        NativeQuery myQuery = NativeQuery.builder()
                .fromClause(playerStatsTable.getName())
                .projectionClause(" ").build();

        QueryResult queryResult = QueryResult.builder().data(DATA).build();
        when(queryEngine.executeQuery(query, qeTransaction)).thenReturn(queryResult);
        when(queryEngine.explain(query)).thenReturn(Arrays.asList(myQuery.toString()));
        AggregationDataStoreTransaction transaction =
                new MyAggregationDataStoreTransaction(queryEngine, cache, queryLogger);
        EntityProjection entityProjection = EntityProjection.builder().type(PlayerStats.class).build();

        assertEquals(DATA, Lists.newArrayList(transaction.loadObjects(entityProjection, scope)));

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
            query = Query.builder().source(playerStatsTable).bypassingCache(true).build();
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
