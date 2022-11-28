/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.yahoo.elide.core.request.Sorting;
import com.yahoo.elide.core.sort.SortingImpl;
import com.yahoo.elide.datastores.aggregation.framework.SQLUnitTest;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.google.common.collect.ImmutableList;
import example.PlayerStats;
import example.dimensions.SubCountry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class SubselectTest extends SQLUnitTest {
    private static final SubCountry SUB_HONG_KONG = new SubCountry();
    private static final SubCountry SUB_USA = new SubCountry();

    @BeforeAll
    public static void init() {
        SQLUnitTest.init();

        SUB_HONG_KONG.setIsoCode("HKG");
        SUB_HONG_KONG.setName("Hong Kong");
        SUB_HONG_KONG.setId("344");

        SUB_USA.setIsoCode("USA");
        SUB_USA.setName("United States");
        SUB_USA.setId("840");
    }

    /**
     * Test grouping by a dimension with a JoinTo annotation.
     *
     * @throws Exception exception
     */
    @Test
    public void testJoinToGroupBy() throws Exception {
        Query query = Query.builder()
                .source(playerStatsTable)
                .metricProjection(playerStatsTable.getMetricProjection("highScore"))
                .dimensionProjection(playerStatsTable.getDimensionProjection("subCountryIsoCode"))
                .build();

        List<Object> results = toList(engine.executeQuery(query, transaction).getData());

        PlayerStats stats1 = new PlayerStats();
        stats1.setId("1");
        stats1.setHighScore(3147483647L);
        stats1.setSubCountryIsoCode("USA");

        PlayerStats stats2 = new PlayerStats();
        stats2.setId("0");
        stats2.setHighScore(1000);
        stats2.setSubCountryIsoCode("HKG");

        assertEquals(ImmutableList.of(stats2, stats1), results);
    }

    /**
     * Test grouping by a dimension with a JoinTo annotation.
     *
     * @throws Exception exception
     */
    @Test
    public void testJoinToFilter() throws Exception {
        Query query = Query.builder()
                .source(playerStatsTable)
                .metricProjection(playerStatsTable.getMetricProjection("highScore"))
                .dimensionProjection(playerStatsTable.getDimensionProjection("overallRating"))
                .whereFilter(filterParser.parseFilterExpression("subCountryIsoCode==USA", playerStatsType, false))
                .build();

        List<Object> results = toList(engine.executeQuery(query, transaction).getData());

        PlayerStats stats1 = new PlayerStats();
        stats1.setId("0");
        stats1.setOverallRating("Good");
        stats1.setHighScore(1234);

        PlayerStats stats2 = new PlayerStats();
        stats2.setId("1");
        stats2.setOverallRating("Great");
        stats2.setHighScore(3147483647L);

        assertEquals(2, results.size());
        assertEquals(stats1, results.get(0));
        assertEquals(stats2, results.get(1));
    }

    /**
     * Test grouping by a dimension with a JoinTo annotation.
     *
     * @throws Exception exception
     */
    @Test
    public void testJoinToSort() throws Exception {
        Map<String, Sorting.SortOrder> sortMap = new TreeMap<>();
        sortMap.put("subCountryIsoCode", Sorting.SortOrder.asc);
        sortMap.put("highScore", Sorting.SortOrder.asc);

        Query query = Query.builder()
                .source(playerStatsTable)
                .metricProjection(playerStatsTable.getMetricProjection("highScore"))
                .dimensionProjection(playerStatsTable.getDimensionProjection("overallRating"))
                .dimensionProjection(playerStatsTable.getDimensionProjection("subCountryIsoCode"))
                .sorting(new SortingImpl(sortMap, PlayerStats.class, dictionary))
                .build();

        List<Object> results = toList(engine.executeQuery(query, transaction).getData());

        PlayerStats stats1 = new PlayerStats();
        stats1.setId("0");
        stats1.setOverallRating("Good");
        stats1.setSubCountryIsoCode("HKG");
        stats1.setHighScore(1000);

        PlayerStats stats2 = new PlayerStats();
        stats2.setId("1");
        stats2.setOverallRating("Good");
        stats2.setSubCountryIsoCode("USA");
        stats2.setHighScore(1234);

        PlayerStats stats3 = new PlayerStats();
        stats3.setId("2");
        stats3.setOverallRating("Great");
        stats3.setSubCountryIsoCode("USA");
        stats3.setHighScore(3147483647L);

        assertEquals(3, results.size());
        assertEquals(stats1, results.get(0));
        assertEquals(stats2, results.get(1));
        assertEquals(stats3, results.get(2));
    }

    //TODO - Add Invalid Request Tests
}
