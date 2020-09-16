/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.yahoo.elide.core.sort.SortingImpl;
import com.yahoo.elide.datastores.aggregation.example.PlayerStats;
import com.yahoo.elide.datastores.aggregation.example.SubCountry;
import com.yahoo.elide.datastores.aggregation.framework.SQLUnitTest;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.request.Sorting;

import com.google.common.collect.ImmutableList;

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
                .table(playerStatsTable)
                .metric(invoke(playerStatsTable.getMetric("highScore")))
                .groupByDimension(toProjection(playerStatsTable.getDimension("subCountryIsoCode")))
                .build();

        List<Object> results = toList(engine.executeQuery(query, transaction).getData());

        PlayerStats stats1 = new PlayerStats();
        stats1.setId("0");
        stats1.setHighScore(2412);
        stats1.setSubCountryIsoCode("USA");

        PlayerStats stats2 = new PlayerStats();
        stats2.setId("1");
        stats2.setHighScore(1000);
        stats2.setSubCountryIsoCode("HKG");

        assertEquals(ImmutableList.of(stats1, stats2), results);
    }

    /**
     * Test grouping by a dimension with a JoinTo annotation.
     *
     * @throws Exception exception
     */
    @Test
    public void testJoinToFilter() throws Exception {
        Query query = Query.builder()
                .table(playerStatsTable)
                .metric(invoke(playerStatsTable.getMetric("highScore")))
                .groupByDimension(toProjection(playerStatsTable.getDimension("overallRating")))
                .whereFilter(filterParser.parseFilterExpression("subCountryIsoCode==USA",
                        PlayerStats.class, false))
                .build();

        List<Object> results = toList(engine.executeQuery(query, transaction).getData());

        PlayerStats stats1 = new PlayerStats();
        stats1.setId("0");
        stats1.setOverallRating("Good");
        stats1.setHighScore(1234);

        PlayerStats stats2 = new PlayerStats();
        stats2.setId("1");
        stats2.setOverallRating("Great");
        stats2.setHighScore(2412);

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
                .table(playerStatsTable)
                .metric(invoke(playerStatsTable.getMetric("highScore")))
                .groupByDimension(toProjection(playerStatsTable.getDimension("overallRating")))
                .groupByDimension(toProjection(playerStatsTable.getDimension("subCountryIsoCode")))
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
        stats3.setHighScore(2412);

        assertEquals(3, results.size());
        assertEquals(stats1, results.get(0));
        assertEquals(stats2, results.get(1));
        assertEquals(stats3, results.get(2));
    }

    //TODO - Add Invalid Request Tests
}
