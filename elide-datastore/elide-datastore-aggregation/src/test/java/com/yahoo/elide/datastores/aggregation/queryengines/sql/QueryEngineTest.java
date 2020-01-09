/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.filter.FilterPredicate;
import com.yahoo.elide.core.filter.Operator;
import com.yahoo.elide.core.pagination.Pagination;
import com.yahoo.elide.core.sort.Sorting;
import com.yahoo.elide.datastores.aggregation.example.PlayerStats;
import com.yahoo.elide.datastores.aggregation.example.PlayerStatsView;
import com.yahoo.elide.datastores.aggregation.framework.SQLUnitTest;
import com.yahoo.elide.datastores.aggregation.metadata.models.AnalyticView;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLAnalyticView;
import com.yahoo.elide.datastores.aggregation.time.TimeGrain;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class QueryEngineTest extends SQLUnitTest {
    private static AnalyticView playerStatsViewTable;

    @BeforeAll
    public static void init() {
        SQLUnitTest.init();

        playerStatsViewTable = new SQLAnalyticView(PlayerStatsView.class, dictionary);
    }

    /**
     * Test loading all three records from the table.
     */
    @Test
    public void testFullTableLoad() {
        Query query = Query.builder()
                .analyticView(playerStatsTable)
                .metric(invoke(playerStatsTable.getMetric("lowScore")))
                .metric(invoke(playerStatsTable.getMetric("highScore")))
                .timeDimension(toProjection(playerStatsTable.getTimeDimension("recordedDate"), TimeGrain.DAY))
                .build();

        List<Object> results = StreamSupport.stream(engine.executeQuery(query).spliterator(), false)
                .collect(Collectors.toList());

        PlayerStats stats0 = new PlayerStats();
        stats0.setId("0");
        stats0.setLowScore(241);
        stats0.setHighScore(2412);
        stats0.setRecordedDate(Timestamp.valueOf("2019-07-11 00:00:00"));

        PlayerStats stats1 = new PlayerStats();
        stats1.setId("1");
        stats1.setLowScore(35);
        stats1.setHighScore(1234);
        stats1.setRecordedDate(Timestamp.valueOf("2019-07-12 00:00:00"));

        PlayerStats stats2 = new PlayerStats();
        stats2.setId("2");
        stats2.setLowScore(72);
        stats2.setHighScore(1000);
        stats2.setRecordedDate(Timestamp.valueOf("2019-07-13 00:00:00"));

        assertEquals(3, results.size());
        assertEquals(stats0, results.get(0));
        assertEquals(stats1, results.get(1));
        assertEquals(stats2, results.get(2));
    }

    /**
     * Test group by a degenerate dimension with a filter applied.
     *
     * @throws Exception exception
     */
    @Test
    public void testDegenerateDimensionFilter() throws Exception {
        Query query = Query.builder()
                .analyticView(playerStatsTable)
                .metric(invoke(playerStatsTable.getMetric("lowScore")))
                .groupByDimension(toProjection(playerStatsTable.getDimension("overallRating")))
                .timeDimension(toProjection(playerStatsTable.getTimeDimension("recordedDate"), TimeGrain.DAY))
                .whereFilter(filterParser.parseFilterExpression("overallRating==Great",
                        PlayerStats.class, false))
                .build();

        List<Object> results = StreamSupport.stream(engine.executeQuery(query).spliterator(), false)
                .collect(Collectors.toList());

        PlayerStats stats1 = new PlayerStats();
        stats1.setId("0");
        stats1.setLowScore(241);
        stats1.setOverallRating("Great");
        stats1.setRecordedDate(Timestamp.valueOf("2019-07-11 00:00:00"));

        assertEquals(1, results.size());
        assertEquals(stats1, results.get(0));
    }

    /**
     * Test filtering on a dimension attribute.
     *
     * @throws Exception exception
     */
    @Test
    public void testFilterJoin() throws Exception {
        Query query = Query.builder()
                .analyticView(playerStatsTable)
                .metric(invoke(playerStatsTable.getMetric("lowScore")))
                .groupByDimension(toProjection(playerStatsTable.getDimension("country")))
                .whereFilter(filterParser.parseFilterExpression("country.name=='United States'",
                        PlayerStats.class, false))
                .build();

        List<Object> results = StreamSupport.stream(engine.executeQuery(query).spliterator(), false)
                .collect(Collectors.toList());

        PlayerStats usa0 = new PlayerStats();
        usa0.setId("0");
        usa0.setLowScore(35);
        usa0.setCountry(USA);

        assertEquals(1, results.size());
        assertEquals(usa0, results.get(0));

        // test relationship hydration
        PlayerStats actualStats1 = (PlayerStats) results.get(0);
        assertNotNull(actualStats1.getCountry());
    }

    /**
     * Test filtering on an attribute that's not present in the query.
     *
     * @throws Exception exception
     */
    @Test
    public void testSubqueryFilterJoin() throws Exception {
        Query query = Query.builder()
                .analyticView(playerStatsViewTable)
                .metric(invoke(playerStatsTable.getMetric("highScore")))
                .whereFilter(filterParser.parseFilterExpression("player.name=='Jane Doe'",
                        PlayerStatsView.class, false))
                .build();

        List<Object> results = StreamSupport.stream(engine.executeQuery(query).spliterator(), false)
                .collect(Collectors.toList());

        PlayerStatsView stats2 = new PlayerStatsView();
        stats2.setId("0");
        stats2.setHighScore(2412);

        assertEquals(1, results.size());
        assertEquals(stats2, results.get(0));
    }

    /**
     * Test a view which filters on "stats.overallRating = 'Great'".
     */
    @Test
    public void testSubqueryLoad() {
        Query query = Query.builder()
                .analyticView(playerStatsViewTable)
                .metric(invoke(playerStatsTable.getMetric("highScore")))
                .build();

        List<Object> results = StreamSupport.stream(engine.executeQuery(query).spliterator(), false)
                .collect(Collectors.toList());

        PlayerStatsView stats2 = new PlayerStatsView();
        stats2.setId("0");
        stats2.setHighScore(2412);

        assertEquals(1, results.size());
        assertEquals(stats2, results.get(0));
    }

    /**
     * Test sorting by dimension attribute which is not present in the query.
     */
    @Test
    public void testSortJoin() {
        Map<String, Sorting.SortOrder> sortMap = new TreeMap<>();
        sortMap.put("player.name", Sorting.SortOrder.asc);

        Query query = Query.builder()
                .analyticView(playerStatsTable)
                .metric(invoke(playerStatsTable.getMetric("lowScore")))
                .groupByDimension(toProjection(playerStatsTable.getDimension("overallRating")))
                .timeDimension(toProjection(playerStatsTable.getTimeDimension("recordedDate"), TimeGrain.DAY))
                .sorting(new Sorting(sortMap))
                .build();

        List<Object> results = StreamSupport.stream(engine.executeQuery(query).spliterator(), false)
                .collect(Collectors.toList());

        PlayerStats stats0 = new PlayerStats();
        stats0.setId("0");
        stats0.setLowScore(72);
        stats0.setOverallRating("Good");
        stats0.setRecordedDate(Timestamp.valueOf("2019-07-13 00:00:00"));

        PlayerStats stats1 = new PlayerStats();
        stats1.setId("1");
        stats1.setLowScore(241);
        stats1.setOverallRating("Great");
        stats1.setRecordedDate(Timestamp.valueOf("2019-07-11 00:00:00"));

        PlayerStats stats2 = new PlayerStats();
        stats2.setId("2");
        stats2.setLowScore(35);
        stats2.setOverallRating("Good");
        stats2.setRecordedDate(Timestamp.valueOf("2019-07-12 00:00:00"));

        assertEquals(3, results.size());
        assertEquals(stats0, results.get(0));
        assertEquals(stats1, results.get(1));
        assertEquals(stats2, results.get(2));
    }

    /**
     * Test pagination.
     */
    @Test
    public void testPagination() {
        Pagination pagination = Pagination.fromOffsetAndLimit(1, 0, true);

        Query query = Query.builder()
                .analyticView(playerStatsTable)
                .metric(invoke(playerStatsTable.getMetric("lowScore")))
                .groupByDimension(toProjection(playerStatsTable.getDimension("overallRating")))
                .timeDimension(toProjection(playerStatsTable.getTimeDimension("recordedDate"), TimeGrain.DAY))
                .pagination(pagination)
                .build();

        List<Object> results = StreamSupport.stream(engine.executeQuery(query).spliterator(), false)
                .collect(Collectors.toList());

        //Jon Doe,1234,72,Good,840,2019-07-12 00:00:00
        PlayerStats stats1 = new PlayerStats();
        stats1.setId("0");
        stats1.setLowScore(35);
        stats1.setOverallRating("Good");
        stats1.setRecordedDate(Timestamp.valueOf("2019-07-12 00:00:00"));

        assertEquals(results.size(), 1, "Number of records returned does not match");
        assertEquals(results.get(0), stats1, "Returned record does not match");
        assertEquals(pagination.getPageTotals(), 3, "Page totals does not match");
    }

    /**
     * Test having clause integrates with group by clause.
     *
     * @throws Exception exception
     */
    @Test
    public void testHavingClause() throws Exception {
        Query query = Query.builder()
                .analyticView(playerStatsTable)
                .metric(invoke(playerStatsTable.getMetric("highScore")))
                .groupByDimension(toProjection(playerStatsTable.getDimension("overallRating")))
                .havingFilter(filterParser.parseFilterExpression("highScore < 2400",
                        PlayerStats.class, false))
                .build();

        List<Object> results = StreamSupport.stream(engine.executeQuery(query).spliterator(), false)
                .collect(Collectors.toList());

        // Only "Good" rating would have total high score less than 2400
        PlayerStats stats1 = new PlayerStats();
        stats1.setId("0");
        stats1.setOverallRating("Good");
        stats1.setHighScore(1234);

        assertEquals(1, results.size());
        assertEquals(stats1, results.get(0));
    }

    /**
     * Test having clause integrates with group by clause and join.
     *
     * @throws Exception exception
     */
    @Test
    public void testHavingClauseJoin() throws Exception {
        Query query = Query.builder()
                .analyticView(playerStatsTable)
                .metric(invoke(playerStatsTable.getMetric("highScore")))
                .groupByDimension(toProjection(playerStatsTable.getDimension("overallRating")))
                .groupByDimension(toProjection(playerStatsTable.getDimension("countryIsoCode")))
                .havingFilter(filterParser.parseFilterExpression("countryIsoCode==USA",
                        PlayerStats.class, false))
                .build();

        List<Object> results = StreamSupport.stream(engine.executeQuery(query).spliterator(), false)
                .collect(Collectors.toList());

        PlayerStats stats0 = new PlayerStats();
        stats0.setId("0");
        stats0.setOverallRating("Great");
        stats0.setCountryIsoCode("USA");
        stats0.setHighScore(2412);

        PlayerStats stats1 = new PlayerStats();
        stats1.setId("1");
        stats1.setOverallRating("Good");
        stats1.setCountryIsoCode("USA");
        stats1.setHighScore(1234);

        assertEquals(2, results.size());
        assertEquals(stats0, results.get(0));
        assertEquals(stats1, results.get(1));
    }

    /**
     * Test group by, having, dimension, metric at the same time.
     *
     * @throws Exception exception
     */
    @Test
    public void testEdgeCasesQuery() throws Exception {
        Map<String, Sorting.SortOrder> sortMap = new TreeMap<>();
        sortMap.put("player.name", Sorting.SortOrder.asc);

        Query query = Query.builder()
                .analyticView(playerStatsViewTable)
                .metric(invoke(playerStatsTable.getMetric("highScore")))
                .groupByDimension(toProjection(playerStatsViewTable.getDimension("countryName")))
                .whereFilter(filterParser.parseFilterExpression("player.name=='Jane Doe'",
                        PlayerStatsView.class, false))
                .havingFilter(filterParser.parseFilterExpression("highScore > 300",
                        PlayerStatsView.class, false))
                .sorting(new Sorting(sortMap))
                .build();

        List<Object> results = StreamSupport.stream(engine.executeQuery(query).spliterator(), false)
                .collect(Collectors.toList());

        PlayerStatsView stats2 = new PlayerStatsView();
        stats2.setId("0");
        stats2.setHighScore(2412);
        stats2.setCountryName("United States");

        assertEquals(1, results.size());
        assertEquals(stats2, results.get(0));
    }

    /**
     * Test sorting by two different columns-one metric and one dimension.
     */
    @Test
    public void testSortByMultipleColumns() {
        Map<String, Sorting.SortOrder> sortMap = new TreeMap<>();
        sortMap.put("lowScore", Sorting.SortOrder.desc);
        sortMap.put("player.name", Sorting.SortOrder.asc);

        Query query = Query.builder()
                .analyticView(playerStatsTable)
                .metric(invoke(playerStatsTable.getMetric("lowScore")))
                .groupByDimension(toProjection(playerStatsTable.getDimension("overallRating")))
                .timeDimension(toProjection(playerStatsTable.getTimeDimension("recordedDate"), TimeGrain.DAY))
                .sorting(new Sorting(sortMap))
                .build();

        List<Object> results = StreamSupport.stream(engine.executeQuery(query).spliterator(), false)
                .collect(Collectors.toList());

        PlayerStats stats0 = new PlayerStats();
        stats0.setId("0");
        stats0.setLowScore(241);
        stats0.setOverallRating("Great");
        stats0.setRecordedDate(Timestamp.valueOf("2019-07-11 00:00:00"));

        PlayerStats stats1 = new PlayerStats();
        stats1.setId("1");
        stats1.setLowScore(72);
        stats1.setOverallRating("Good");
        stats1.setRecordedDate(Timestamp.valueOf("2019-07-13 00:00:00"));

        PlayerStats stats2 = new PlayerStats();
        stats2.setId("2");
        stats2.setLowScore(35);
        stats2.setOverallRating("Good");
        stats2.setRecordedDate(Timestamp.valueOf("2019-07-12 00:00:00"));

        assertEquals(3, results.size());
        assertEquals(stats0, results.get(0));
        assertEquals(stats1, results.get(1));
        assertEquals(stats2, results.get(2));
    }

    /**
     * Test hydrating multiple relationship values. Make sure the objects are constructed correctly.
     */
    @Test
    public void testRelationshipHydration() {
        Map<String, Sorting.SortOrder> sortMap = new TreeMap<>();
        sortMap.put("country.name", Sorting.SortOrder.desc);
        sortMap.put("overallRating", Sorting.SortOrder.desc);

        Query query = Query.builder()
                .analyticView(playerStatsTable)
                .metric(invoke(playerStatsTable.getMetric("lowScore")))
                .groupByDimension(toProjection(playerStatsTable.getDimension("overallRating")))
                .groupByDimension(toProjection(playerStatsTable.getDimension("country")))
                .sorting(new Sorting(sortMap))
                .build();

        List<Object> results = StreamSupport.stream(engine.executeQuery(query).spliterator(), false)
                .collect(Collectors.toList());

        PlayerStats usa0 = new PlayerStats();
        usa0.setId("0");
        usa0.setLowScore(241);
        usa0.setOverallRating("Great");
        usa0.setCountry(USA);

        PlayerStats usa1 = new PlayerStats();
        usa1.setId("1");
        usa1.setLowScore(35);
        usa1.setOverallRating("Good");
        usa1.setCountry(USA);

        PlayerStats hk2 = new PlayerStats();
        hk2.setId("2");
        hk2.setLowScore(72);
        hk2.setOverallRating("Good");
        hk2.setCountry(HONG_KONG);

        assertEquals(3, results.size());
        assertEquals(usa0, results.get(0));
        assertEquals(usa1, results.get(1));
        assertEquals(hk2, results.get(2));

        // test join
        PlayerStats actualStats1 = (PlayerStats) results.get(0);
        assertNotNull(actualStats1.getCountry());
    }

    /**
     * Test grouping by a dimension with a JoinTo annotation.
     */
    @Test
    public void testJoinToGroupBy() {
        Query query = Query.builder()
                .analyticView(playerStatsTable)
                .metric(invoke(playerStatsTable.getMetric("highScore")))
                .groupByDimension(toProjection(playerStatsTable.getDimension("countryIsoCode")))
                .build();

        List<Object> results = StreamSupport.stream(engine.executeQuery(query).spliterator(), false)
                .collect(Collectors.toList());

        PlayerStats stats1 = new PlayerStats();
        stats1.setId("0");
        stats1.setHighScore(2412);
        stats1.setCountryIsoCode("USA");

        PlayerStats stats2 = new PlayerStats();
        stats2.setId("1");
        stats2.setHighScore(1000);
        stats2.setCountryIsoCode("HKG");

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
                .analyticView(playerStatsTable)
                .metric(invoke(playerStatsTable.getMetric("highScore")))
                .groupByDimension(toProjection(playerStatsTable.getDimension("overallRating")))
                .whereFilter(filterParser.parseFilterExpression("countryIsoCode==USA",
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
     */
    @Test
    public void testJoinToSort() {
        Map<String, Sorting.SortOrder> sortMap = new TreeMap<>();
        sortMap.put("countryIsoCode", Sorting.SortOrder.asc);

        Query query = Query.builder()
                .analyticView(playerStatsTable)
                .metric(invoke(playerStatsTable.getMetric("highScore")))
                .groupByDimension(toProjection(playerStatsTable.getDimension("overallRating")))
                .groupByDimension(toProjection(playerStatsTable.getDimension("country")))
                .sorting(new Sorting(sortMap))
                .build();

        List<Object> results = StreamSupport.stream(engine.executeQuery(query).spliterator(), false)
                .collect(Collectors.toList());

        PlayerStats stats1 = new PlayerStats();
        stats1.setId("0");
        stats1.setOverallRating("Good");
        stats1.setCountry(HONG_KONG);
        stats1.setHighScore(1000);

        PlayerStats stats2 = new PlayerStats();
        stats2.setId("1");
        stats2.setOverallRating("Good");
        stats2.setCountry(USA);
        stats2.setHighScore(1234);

        PlayerStats stats3 = new PlayerStats();
        stats3.setId("2");
        stats3.setOverallRating("Great");
        stats3.setCountry(USA);
        stats3.setHighScore(2412);

        assertEquals(3, results.size());
        assertEquals(stats1, results.get(0));
        assertEquals(stats2, results.get(1));
        assertEquals(stats3, results.get(2));
    }

    /**
     * Test month grain query.
     */
    @Test
    public void testTotalScoreByMonth() {
        Query query = Query.builder()
                .analyticView(playerStatsTable)
                .metric(invoke(playerStatsTable.getMetric("highScore")))
                .timeDimension(toProjection(playerStatsTable.getTimeDimension("recordedDate"), TimeGrain.MONTH))
                .build();

        List<Object> results = StreamSupport.stream(engine.executeQuery(query).spliterator(), false)
                .collect(Collectors.toList());

        PlayerStats stats0 = new PlayerStats();
        stats0.setId("0");
        stats0.setHighScore(2412);
        stats0.setRecordedDate(Timestamp.valueOf("2019-07-01 00:00:00"));

        assertEquals(1, results.size());
        assertEquals(stats0, results.get(0));
    }

    /**
     * Test filter by time dimension.
     */
    @Test
    public void testFilterByTemporalDimension() {
        FilterPredicate predicate = new FilterPredicate(
                new Path(PlayerStats.class, dictionary, "recordedDate"),
                Operator.IN,
                Lists.newArrayList(Timestamp.valueOf("2019-07-11 00:00:00")));

        Query query = Query.builder()
                .analyticView(playerStatsTable)
                .metric(invoke(playerStatsTable.getMetric("highScore")))
                .timeDimension(toProjection(playerStatsTable.getTimeDimension("recordedDate"), TimeGrain.DAY))
                .whereFilter(predicate)
                .build();

        List<Object> results = StreamSupport.stream(engine.executeQuery(query).spliterator(), false)
                .collect(Collectors.toList());

        PlayerStats stats0 = new PlayerStats();
        stats0.setId("0");
        stats0.setHighScore(2412);
        stats0.setRecordedDate(Timestamp.valueOf("2019-07-11 00:00:00"));

        assertEquals(1, results.size());
        assertEquals(stats0, results.get(0));
    }

    @Test
    public void testSortAggregatedMetric() {
        Map<String, Sorting.SortOrder> sortMap = new TreeMap<>();
        sortMap.put("lowScore", Sorting.SortOrder.desc);

        Query query = Query.builder()
                .analyticView(playerStatsTable)
                .groupByDimension(toProjection(playerStatsTable.getDimension("overallRating")))
                .metric(invoke(playerStatsTable.getMetric("lowScore")))
                .sorting(new Sorting(sortMap))
                .build();

        List<Object> results = StreamSupport.stream(engine.executeQuery(query).spliterator(), false)
                .collect(Collectors.toList());

        PlayerStats stats0 = new PlayerStats();
        stats0.setId("0");
        stats0.setLowScore(241);
        stats0.setOverallRating("Great");

        PlayerStats stats1 = new PlayerStats();
        stats1.setId("1");
        stats1.setLowScore(35);
        stats1.setOverallRating("Good");

        assertEquals(2, results.size());
        assertEquals(stats0, results.get(0));
        assertEquals(stats1, results.get(1));
    }

    @Test
    public void testAmbiguousFields() {
        Map<String, Sorting.SortOrder> sortMap = new TreeMap<>();
        sortMap.put("lowScore", Sorting.SortOrder.asc);

        Query query = Query.builder()
                .analyticView(playerStatsTable)
                .groupByDimension(toProjection(playerStatsTable.getDimension("playerName")))
                .groupByDimension(toProjection(playerStatsTable.getDimension("player2Name")))
                .metric(invoke(playerStatsTable.getMetric("lowScore")))
                .sorting(new Sorting(sortMap))
                .build();

        List<Object> results = StreamSupport.stream(engine.executeQuery(query).spliterator(), false)
                .collect(Collectors.toList());

        PlayerStats stats0 = new PlayerStats();
        stats0.setId("0");
        stats0.setLowScore(35);
        stats0.setPlayerName("Jon Doe");
        stats0.setPlayer2Name("Jane Doe");

        PlayerStats stats1 = new PlayerStats();
        stats1.setId("1");
        stats1.setLowScore(72);
        stats1.setPlayerName("Han");
        stats1.setPlayer2Name("Jon Doe");

        PlayerStats stats2 = new PlayerStats();
        stats2.setId("2");
        stats2.setLowScore(241);
        stats2.setPlayerName("Jane Doe");
        stats2.setPlayer2Name("Han");

        assertEquals(3, results.size());
        assertEquals(stats0, results.get(0));
        assertEquals(stats1, results.get(1));
        assertEquals(stats2, results.get(2));
    }

    //TODO - Add Invalid Request Tests
}
