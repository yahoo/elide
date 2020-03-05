/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.filter.FilterPredicate;
import com.yahoo.elide.core.filter.Operator;
import com.yahoo.elide.core.pagination.PaginationImpl;
import com.yahoo.elide.core.sort.SortingImpl;
import com.yahoo.elide.datastores.aggregation.example.PlayerStats;
import com.yahoo.elide.datastores.aggregation.example.PlayerStatsView;
import com.yahoo.elide.datastores.aggregation.framework.SQLUnitTest;
import com.yahoo.elide.datastores.aggregation.metadata.enums.TimeGrain;
import com.yahoo.elide.datastores.aggregation.metadata.models.Table;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromSubquery;
import com.yahoo.elide.request.Sorting;

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
    private static Table playerStatsViewTable;

    @BeforeAll
    public static void init() {
        SQLUnitTest.init();

        playerStatsViewTable = engine.getTable("playerStatsView");
    }

    /**
     * Test loading all three records from the table.
     */
    @Test
    public void testFullTableLoad() {
        Query query = Query.builder()
                .table(playerStatsTable)
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
     * Test loading records using {@link FromSubquery}
     */
    @Test
    public void testFromSubQuery() {
        Query query = Query.builder()
                .table(playerStatsViewTable)
                .metric(invoke(playerStatsViewTable.getMetric("highScore")))
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
     * Test group by, having, dimension, metric at the same time.
     *
     * @throws Exception exception
     */
    @Test
    public void testAllArgumentQuery() throws Exception {
        Map<String, Sorting.SortOrder> sortMap = new TreeMap<>();
        sortMap.put("countryName", Sorting.SortOrder.asc);

        Query query = Query.builder()
                .table(playerStatsViewTable)
                .metric(invoke(playerStatsViewTable.getMetric("highScore")))
                .groupByDimension(toProjection(playerStatsViewTable.getDimension("countryName")))
                .whereFilter(filterParser.parseFilterExpression("countryName=='United States'",
                        PlayerStatsView.class, false))
                .havingFilter(filterParser.parseFilterExpression("highScore > 300",
                        PlayerStatsView.class, false))
                .sorting(new SortingImpl(sortMap, PlayerStatsView.class, dictionary))
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
     * Test group by a degenerate dimension with a filter applied.
     *
     * @throws Exception exception
     */
    @Test
    public void testDegenerateDimensionFilter() throws Exception {
        Query query = Query.builder()
                .table(playerStatsTable)
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
     * Test filtering on an attribute that's not present in the query.
     *
     * @throws Exception exception
     */
    @Test
    public void testNotProjectedFilter() throws Exception {
        Query query = Query.builder()
                .table(playerStatsViewTable)
                .metric(invoke(playerStatsViewTable.getMetric("highScore")))
                .whereFilter(filterParser.parseFilterExpression("countryName=='United States'",
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

    @Test
    public void testSortAggregatedMetric() {
        Map<String, Sorting.SortOrder> sortMap = new TreeMap<>();
        sortMap.put("lowScore", Sorting.SortOrder.desc);

        Query query = Query.builder()
                .table(playerStatsTable)
                .groupByDimension(toProjection(playerStatsTable.getDimension("overallRating")))
                .metric(invoke(playerStatsTable.getMetric("lowScore")))
                .sorting(new SortingImpl(sortMap, PlayerStats.class, dictionary))
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

    /**
     * Test sorting by dimension attribute which is not present in the query.
     */
    @Test
    public void testSortJoin() {
        Map<String, Sorting.SortOrder> sortMap = new TreeMap<>();
        sortMap.put("playerName", Sorting.SortOrder.asc);

        Query query = Query.builder()
                .table(playerStatsTable)
                .metric(invoke(playerStatsTable.getMetric("lowScore")))
                .groupByDimension(toProjection(playerStatsTable.getDimension("overallRating")))
                .timeDimension(toProjection(playerStatsTable.getTimeDimension("recordedDate"), TimeGrain.DAY))
                .sorting(new SortingImpl(sortMap, PlayerStats.class, dictionary))
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
        PaginationImpl pagination = new PaginationImpl(
                PlayerStats.class,
                0,
                1,
                PaginationImpl.DEFAULT_PAGE_LIMIT,
                PaginationImpl.MAX_PAGE_LIMIT,
                true,
                false
        );

        Query query = Query.builder()
                .table(playerStatsTable)
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
                .table(playerStatsTable)
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
                .table(playerStatsTable)
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
     * Test sorting by two different columns-one metric and one dimension.
     */
    @Test
    public void testSortByMultipleColumns() {
        Map<String, Sorting.SortOrder> sortMap = new TreeMap<>();
        sortMap.put("lowScore", Sorting.SortOrder.desc);
        sortMap.put("playerName", Sorting.SortOrder.asc);

        Query query = Query.builder()
                .table(playerStatsTable)
                .metric(invoke(playerStatsTable.getMetric("lowScore")))
                .groupByDimension(toProjection(playerStatsTable.getDimension("overallRating")))
                .timeDimension(toProjection(playerStatsTable.getTimeDimension("recordedDate"), TimeGrain.DAY))
                .sorting(new SortingImpl(sortMap, PlayerStats.class, dictionary))
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
     * Test grouping by a dimension with a JoinTo annotation.
     */
    @Test
    public void testJoinToGroupBy() {
        Query query = Query.builder()
                .table(playerStatsTable)
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
                .table(playerStatsTable)
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
        sortMap.put("highScore", Sorting.SortOrder.asc);

        Query query = Query.builder()
                .table(playerStatsTable)
                .metric(invoke(playerStatsTable.getMetric("highScore")))
                .groupByDimension(toProjection(playerStatsTable.getDimension("overallRating")))
                .groupByDimension(toProjection(playerStatsTable.getDimension("countryIsoCode")))
                .sorting(new SortingImpl(sortMap, PlayerStats.class, dictionary))
                .build();

        List<Object> results = StreamSupport.stream(engine.executeQuery(query).spliterator(), false)
                .collect(Collectors.toList());

        PlayerStats stats1 = new PlayerStats();
        stats1.setId("0");
        stats1.setOverallRating("Good");
        stats1.setCountryIsoCode("HKG");
        stats1.setHighScore(1000);

        PlayerStats stats2 = new PlayerStats();
        stats2.setId("1");
        stats2.setOverallRating("Good");
        stats2.setCountryIsoCode("USA");
        stats2.setHighScore(1234);

        PlayerStats stats3 = new PlayerStats();
        stats3.setId("2");
        stats3.setOverallRating("Great");
        stats3.setCountryIsoCode("USA");
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
                .table(playerStatsTable)
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
                .table(playerStatsTable)
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
    public void testAmbiguousFields() {
        Map<String, Sorting.SortOrder> sortMap = new TreeMap<>();
        sortMap.put("lowScore", Sorting.SortOrder.asc);

        Query query = Query.builder()
                .table(playerStatsTable)
                .groupByDimension(toProjection(playerStatsTable.getDimension("playerName")))
                .groupByDimension(toProjection(playerStatsTable.getDimension("player2Name")))
                .metric(invoke(playerStatsTable.getMetric("lowScore")))
                .sorting(new SortingImpl(sortMap, PlayerStats.class, dictionary))
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
