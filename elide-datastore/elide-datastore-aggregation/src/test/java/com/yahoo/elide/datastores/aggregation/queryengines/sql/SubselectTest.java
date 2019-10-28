/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.yahoo.elide.core.sort.Sorting;
import com.yahoo.elide.datastores.aggregation.QueryEngine;
import com.yahoo.elide.datastores.aggregation.example.PlayerStats;
import com.yahoo.elide.datastores.aggregation.example.SubCountry;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.datastores.aggregation.query.TimeDimensionProjection;
import com.yahoo.elide.datastores.aggregation.schema.Schema;
import com.yahoo.elide.datastores.aggregation.schema.dimension.TimeDimensionColumn;
import com.yahoo.elide.datastores.aggregation.schema.metric.Sum;
import com.yahoo.elide.datastores.aggregation.time.TimeGrain;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class SubselectTest extends UnitTest {
    private static final SubCountry SUB_HONG_KONG = new SubCountry();
    private static final SubCountry SUB_USA = new SubCountry();

    @BeforeAll
    public static void init() {
        QueryEngineTest.init();

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
        QueryEngine engine = new SQLQueryEngine(emf, dictionary);

        Query query = Query.builder()
                .schema(playerStatsSchema)
                .metric(playerStatsSchema.getMetric("lowScore"), Sum.class)
                .metric(playerStatsSchema.getMetric("highScore"), Sum.class)
                .groupDimension(playerStatsSchema.getDimension("overallRating"))
                .groupDimension(playerStatsSchema.getDimension("subCountry"))
                .groupDimension(playerStatsSchema.getDimension("country"))
                .timeDimension(toTimeDimension(playerStatsSchema, TimeGrain.DAY, "recordedDate"))
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
        QueryEngine engine = new SQLQueryEngine(emf, dictionary);

        Map<String, Sorting.SortOrder> sortMap = new TreeMap<>();
        sortMap.put("subCountry.name", Sorting.SortOrder.desc);

        Query query = Query.builder()
                .schema(playerStatsSchema)
                .metric(playerStatsSchema.getMetric("lowScore"), Sum.class)
                .metric(playerStatsSchema.getMetric("highScore"), Sum.class)
                .groupDimension(playerStatsSchema.getDimension("overallRating"))
                .groupDimension(playerStatsSchema.getDimension("country"))
                .groupDimension(playerStatsSchema.getDimension("subCountry"))
                .timeDimension(toTimeDimension(playerStatsSchema, TimeGrain.DAY, "recordedDate"))
                .sorting(new Sorting(sortMap))
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
        QueryEngine engine = new SQLQueryEngine(emf, dictionary);

        Query query = Query.builder()
                .schema(playerStatsSchema)
                .metric(playerStatsSchema.getMetric("highScore"), Sum.class)
                .groupDimension(playerStatsSchema.getDimension("subCountryIsoCode"))
                .build();

        List<Object> results = StreamSupport.stream(engine.executeQuery(query).spliterator(), false)
                .collect(Collectors.toList());

        PlayerStats stats1 = new PlayerStats();
        stats1.setId("0");
        stats1.setHighScore(3646);
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
        QueryEngine engine = new SQLQueryEngine(emf, dictionary);

        Query query = Query.builder()
                .schema(playerStatsSchema)
                .metric(playerStatsSchema.getMetric("highScore"), Sum.class)
                .groupDimension(playerStatsSchema.getDimension("overallRating"))
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
        QueryEngine engine = new SQLQueryEngine(emf, dictionary);

        Map<String, Sorting.SortOrder> sortMap = new TreeMap<>();
        sortMap.put("subCountryIsoCode", Sorting.SortOrder.asc);

        Query query = Query.builder()
                .schema(playerStatsSchema)
                .metric(playerStatsSchema.getMetric("highScore"), Sum.class)
                .groupDimension(playerStatsSchema.getDimension("overallRating"))
                .groupDimension(playerStatsSchema.getDimension("subCountry"))
                .sorting(new Sorting(sortMap))
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

    /**
     * Searches the schema for a time dimension column that matches the requested column name and time grain.
     * @param grain The column time grain requested.
     * @param dimensionName The name of the column.
     * @return A newly constructed requested time dimension with the matching grain.
     */
    private static TimeDimensionProjection toTimeDimension(Schema schema, TimeGrain grain, String dimensionName) {
        TimeDimensionColumn column = schema.getTimeDimension(dimensionName);

        if (column == null) {
            return null;
        }

        return column.getSupportedGrains().stream()
                .filter(supportedGrain -> supportedGrain.grain().equals(grain))
                .findFirst()
                .map(supportedGrain -> column.toProjectedDimension(supportedGrain))
                .orElse(null);
    }
}
