/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.yahoo.elide.datastores.aggregation.framework.SQLUnitTest;
import com.yahoo.elide.datastores.aggregation.metadata.enums.TimeGrain;
import com.yahoo.elide.datastores.aggregation.query.Cache;
import com.yahoo.elide.datastores.aggregation.query.ImmutablePagination;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.datastores.aggregation.query.QueryResult;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.StreamSupport;

import javax.persistence.EntityManager;

public class QueryEngineWithCacheTest extends SQLUnitTest {

    private ConcurrentHashMap<Object, QueryResult> cache;

    @BeforeEach
    public void setUp() {
        cache = new ConcurrentHashMap<>();
        SQLUnitTest.init(new Cache() {
            @Override
            public QueryResult get(Object key) {
                return cache.get(key);
            }

            @Override
            public void put(Object key, QueryResult result) {
                cache.put(key, result);
            }
        });
    }

    private Query buildQuery(boolean bypassCache) {
        return Query.builder()
                .table(playerStatsTable)
                .metric(invoke(playerStatsTable.getMetric("lowScore")))
                .metric(invoke(playerStatsTable.getMetric("highScore")))
                .timeDimension(toProjection(playerStatsTable.getTimeDimension("recordedDate"), TimeGrain.DAY))
                .pagination(new ImmutablePagination(0, 1, false, true))
                .bypassingCache(bypassCache)
                .build();
    }

    @Test
    public void testCacheUsed() {
        Query query = buildQuery(false);

        QueryResult result = engine.executeQuery(query);
        assertEquals(1, StreamSupport.stream(result.getData().spliterator(), false).count());
        assertEquals(1, cache.size(), "Expected cache entry for query");
        assertEquals(3, result.getPageTotals(), "Page totals does not match");

        query = buildQuery(false);
        QueryResult result2 = engine.executeQuery(query);
        assertSame(result, result2, "Expected results from cache");
        assertEquals(1, cache.size(), "Expected to re-use cache entry");
        assertEquals(3, result2.getPageTotals(), "Expected page total set");
    }

    @Test
    public void testCacheInvalidated() {
        Query query = buildQuery(false);

        QueryResult result = engine.executeQuery(query);
        assertEquals(1, StreamSupport.stream(result.getData().spliterator(), false).count());
        assertEquals(1, cache.size(), "Expected cache entry for query");
        assertEquals(3, result.getPageTotals(), "Page totals does not match");

        // now add new PlayerStats row
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.createNativeQuery(
                "INSERT INTO playerStats VALUES(123, 123, 'Good', 344, 344, 3, 1, '2020-01-01 00:00:00');"
        ).executeUpdate();
        em.getTransaction().commit();

        query = buildQuery(false);
        QueryResult result2 = engine.executeQuery(query);
        assertNotSame(result, result2, "Expected different results");
        assertEquals(2, cache.size(), "Expected to add updated cache entry");
        assertEquals(4, result2.getPageTotals(), "Expected page total set");
    }

    @Test
    public void testCacheBypassed() {
        Query query = buildQuery(true);

        QueryResult result = engine.executeQuery(query);
        assertEquals(1, StreamSupport.stream(result.getData().spliterator(), false).count());
        assertEquals(0, cache.size(), "Expected no cache entries added");

        QueryResult result2 = engine.executeQuery(query);
        assertNotSame(result, result2);
        assertEquals(1, StreamSupport.stream(result.getData().spliterator(), false).count());
        assertEquals(0, cache.size(), "Expected no cache entries added");
    }
}
