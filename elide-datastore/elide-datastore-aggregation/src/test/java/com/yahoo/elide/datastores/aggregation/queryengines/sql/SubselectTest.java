/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.yahoo.elide.core.sort.SortingImpl;
import com.yahoo.elide.datastores.aggregation.example.PlayerStats;
import com.yahoo.elide.datastores.aggregation.example.SubCountry;
import com.yahoo.elide.datastores.aggregation.framework.SQLUnitTest;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.datastores.aggregation.time.TimeGrain;

import com.yahoo.elide.request.Sorting;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

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
     * Test filtering on a dimension attribute.
     *
     * @throws Exception exception
     */
    @Test
    public void testFilterJoin() throws Exception {
        Query query = Query.builder()
                .table(playerStatsTable)
                .metric(invoke(playerStatsTable.getMetric("lowScore")))
                .metric(invoke(playerStatsTable.getMetric("highScore")))
                .groupByDimension(toProjection(playerStatsTable.getDimension("overallRating")))
                .groupByDimension(toProjection(playerStatsTable.getDimension("subCountry")))
                .groupByDimension(toProjection(playerStatsTable.getDimension("country")))
                .timeDimension(toProjection(playerStatsTable.getTimeDimension("recordedDate"), TimeGrain.DAY))
                .whereFilter(filterParser.parseFilterExpression("subCountry.name=='United States'",
                        PlayerStats.class, false))
                .build();


        List<Object> results = StreamSupport.stream(engine.executeQuery(query).spliterator(), false)
                .collect(Collectors.toList());

        PlayerStats usa0 = new PlayerStats();
        usa0.setId("0");
        usa0.setLowScore(35);
        usa0.setHighScore(1234);
        usa0.setOverallRating("Good");
        usa0.setCountry(USA);
        usa0.setSubCountry(SUB_USA);
        usa0.setRecordedDate(Timestamp.valueOf("2019-07-12 00:00:00"));

        PlayerStats usa1 = new PlayerStats();
        usa1.setId("1");
        usa1.setLowScore(241);
        usa1.setHighScore(2412);
        usa1.setOverallRating("Great");
        usa1.setCountry(USA);
        usa1.setSubCountry(SUB_USA);
        usa1.setRecordedDate(Timestamp.valueOf("2019-07-11 00:00:00"));

        assertEquals(2, results.size());
        assertEquals(usa0, results.get(0));
        assertEquals(usa1, results.get(1));

        // test join
        PlayerStats actualStats0 = (PlayerStats) results.get(0);
        assertNotNull(actualStats0.getSubCountry());
        assertNotNull(actualStats0.getCountry());
    }

    /**
     * Test hydrating multiple relationship values. Make sure the objects are constructed correctly.
     */
    @Test
    public void testRelationshipHydration() {
        Map<String, Sorting.SortOrder> sortMap = new TreeMap<>();
        sortMap.put("subCountry.name", Sorting.SortOrder.desc);

        Query query = Query.builder()
                .table(playerStatsTable)
                .metric(invoke(playerStatsTable.getMetric("lowScore")))
                .metric(invoke(playerStatsTable.getMetric("highScore")))
                .groupByDimension(toProjection(playerStatsTable.getDimension("overallRating")))
                .groupByDimension(toProjection(playerStatsTable.getDimension("country")))
                .groupByDimension(toProjection(playerStatsTable.getDimension("subCountry")))
                .timeDimension(toProjection(playerStatsTable.getTimeDimension("recordedDate"), TimeGrain.DAY))
                .sorting(new SortingImpl(sortMap, PlayerStats.class, dictionary))
                .build();

        List<Object> results = StreamSupport.stream(engine.executeQuery(query).spliterator(), false)
                .collect(Collectors.toList());

        PlayerStats usa0 = new PlayerStats();
        usa0.setId("0");
        usa0.setLowScore(35);
        usa0.setHighScore(1234);
        usa0.setOverallRating("Good");
        usa0.setCountry(USA);
        usa0.setSubCountry(SUB_USA);
        usa0.setRecordedDate(Timestamp.valueOf("2019-07-12 00:00:00"));

        PlayerStats usa1 = new PlayerStats();
        usa1.setId("1");
        usa1.setLowScore(241);
        usa1.setHighScore(2412);
        usa1.setOverallRating("Great");
        usa1.setCountry(USA);
        usa1.setSubCountry(SUB_USA);
        usa1.setRecordedDate(Timestamp.valueOf("2019-07-11 00:00:00"));

        PlayerStats hk2 = new PlayerStats();
        hk2.setId("2");
        hk2.setLowScore(72);
        hk2.setHighScore(1000);
        hk2.setOverallRating("Good");
        hk2.setCountry(HONG_KONG);
        hk2.setSubCountry(SUB_HONG_KONG);
        hk2.setRecordedDate(Timestamp.valueOf("2019-07-13 00:00:00"));

        assertEquals(3, results.size());
        assertEquals(usa0, results.get(0));
        assertEquals(usa1, results.get(1));
        assertEquals(hk2, results.get(2));

        // test join
        PlayerStats actualStats0 = (PlayerStats) results.get(0);
        assertNotNull(actualStats0.getSubCountry());
        assertNotNull(actualStats0.getCountry());
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

        List<Object> results = StreamSupport.stream(engine.executeQuery(query).spliterator(), false)
                .collect(Collectors.toList());

        PlayerStats stats1 = new PlayerStats();
        stats1.setId("0");
        stats1.setHighScore(2412);
        stats1.setSubCountryIsoCode("USA");

        PlayerStats stats2 = new PlayerStats();
        stats2.setId("1");
        stats2.setHighScore(1000);
        stats2.setSubCountryIsoCode("HKG");

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
    public void testJoinToFilter() throws Exception {
        Query query = Query.builder()
                .table(playerStatsTable)
                .metric(invoke(playerStatsTable.getMetric("highScore")))
                .groupByDimension(toProjection(playerStatsTable.getDimension("overallRating")))
                .whereFilter(filterParser.parseFilterExpression("subCountryIsoCode==USA",
                        PlayerStats.class, false))
                .build();

        List<Object> results = StreamSupport.stream(engine.executeQuery(query).spliterator(), false)
                .collect(Collectors.toList());

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

        Query query = Query.builder()
                .table(playerStatsTable)
                .metric(invoke(playerStatsTable.getMetric("highScore")))
                .groupByDimension(toProjection(playerStatsTable.getDimension("overallRating")))
                .groupByDimension(toProjection(playerStatsTable.getDimension("subCountry")))
                .sorting(new SortingImpl(sortMap, PlayerStats.class, dictionary))
                .build();

        List<Object> results = StreamSupport.stream(engine.executeQuery(query).spliterator(), false)
                .collect(Collectors.toList());

        PlayerStats stats1 = new PlayerStats();
        stats1.setId("0");
        stats1.setOverallRating("Good");
        stats1.setSubCountry(SUB_HONG_KONG);
        stats1.setHighScore(1000);

        PlayerStats stats2 = new PlayerStats();
        stats2.setId("1");
        stats2.setOverallRating("Good");
        stats2.setSubCountry(SUB_USA);
        stats2.setHighScore(1234);

        PlayerStats stats3 = new PlayerStats();
        stats3.setId("2");
        stats3.setOverallRating("Great");
        stats3.setSubCountry(SUB_USA);
        stats3.setHighScore(2412);

        assertEquals(3, results.size());
        assertEquals(stats1, results.get(0));
        assertEquals(stats2, results.get(1));
        assertEquals(stats3, results.get(2));
    }

    //TODO - Add Invalid Request Tests
}
