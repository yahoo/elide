/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql;

import static com.yahoo.elide.datastores.aggregation.timegrains.Time.TIME_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.exceptions.InvalidParameterizedAttributeException;
import com.yahoo.elide.core.filter.Operator;
import com.yahoo.elide.core.filter.predicates.FilterPredicate;
import com.yahoo.elide.core.request.Argument;
import com.yahoo.elide.core.request.Attribute;
import com.yahoo.elide.core.request.Sorting;
import com.yahoo.elide.core.sort.SortingImpl;
import com.yahoo.elide.core.type.ClassType;
import com.yahoo.elide.datastores.aggregation.framework.SQLUnitTest;
import com.yahoo.elide.datastores.aggregation.metadata.enums.TimeGrain;
import com.yahoo.elide.datastores.aggregation.query.ImmutablePagination;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.datastores.aggregation.query.QueryResult;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromSubquery;
import com.yahoo.elide.datastores.aggregation.timegrains.Day;
import com.yahoo.elide.datastores.aggregation.timegrains.Month;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import example.PlayerStats;
import example.PlayerStatsView;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Date;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class QueryEngineTest extends SQLUnitTest {

    @BeforeAll
    public static void init() {
        SQLUnitTest.init();
    }

    /**
     * Test loading all three records from the table.
     *
     * @throws Exception exception
     */
    @Test
    public void testFullTableLoad() throws Exception {
        Query query = Query.builder()
                .source(playerStatsTable)
                .metricProjection(playerStatsTable.getMetricProjection("lowScore"))
                .metricProjection(playerStatsTable.getMetricProjection("highScore"))
                .timeDimensionProjection(playerStatsTable.getTimeDimensionProjection("recordedDate"))
                .build();

        List<Object> results = toList(engine.executeQuery(query, transaction).getData());

        PlayerStats stats0 = new PlayerStats();
        stats0.setId("0");
        stats0.setLowScore(241);
        stats0.setHighScore(3147483647L);
        stats0.setRecordedDate(new Day(Date.valueOf("2019-07-11")));

        PlayerStats stats1 = new PlayerStats();
        stats1.setId("1");
        stats1.setLowScore(35);
        stats1.setHighScore(1234);
        stats1.setRecordedDate(new Day(Date.valueOf("2019-07-12")));

        PlayerStats stats2 = new PlayerStats();
        stats2.setId("2");
        stats2.setLowScore(72);
        stats2.setHighScore(1000);
        stats2.setRecordedDate(new Day(Date.valueOf("2019-07-13")));

        assertEquals(ImmutableList.of(stats0, stats1, stats2), results);
    }

    /**
     * Test loading records using {@link FromSubquery}.
     *
     * @throws Exception exception
     */
    @Test
    public void testFromSubQuery() throws Exception {
        Query query = Query.builder()
                .source(playerStatsViewTable.toQueryable())
                .metricProjection(playerStatsViewTable.getMetricProjection("highScore"))
                .arguments(playerStatsViewTableArgs)
                .build();

        List<Object> results = toList(engine.executeQuery(query, transaction).getData());

        PlayerStatsView stats2 = new PlayerStatsView();
        stats2.setId("0");
        stats2.setHighScore(3147483647L);

        assertEquals(ImmutableList.of(stats2), results);
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
                .source(playerStatsViewTable)
                .metricProjection(playerStatsViewTable.getMetricProjection("highScore"))
                .dimensionProjection(playerStatsViewTable.getDimensionProjection("countryName"))
                .whereFilter(filterParser.parseFilterExpression("countryName=='United States'", playerStatsViewType, false))
                .havingFilter(filterParser.parseFilterExpression("highScore > 300", playerStatsViewType, false))
                .sorting(new SortingImpl(sortMap, PlayerStatsView.class, dictionary))
                .arguments(playerStatsViewTableArgs)
                .build();

        List<Object> results = toList(engine.executeQuery(query, transaction).getData());

        PlayerStatsView stats2 = new PlayerStatsView();
        stats2.setId("0");
        stats2.setHighScore(3147483647L);
        stats2.setCountryName("United States");

        assertEquals(ImmutableList.of(stats2), results);
    }

    /**
     * Test group by a degenerate dimension with a filter applied.
     *
     * @throws Exception exception
     */
    @Test
    public void testDegenerateDimensionFilter() throws Exception {
        Query query = Query.builder()
                .source(playerStatsTable)
                .metricProjection(playerStatsTable.getMetricProjection("lowScore"))
                .dimensionProjection(playerStatsTable.getDimensionProjection("overallRating"))
                .timeDimensionProjection(playerStatsTable.getTimeDimensionProjection("recordedDate"))
                .whereFilter(filterParser.parseFilterExpression("overallRating==Great", playerStatsType, false))
                .build();

        List<Object> results = toList(engine.executeQuery(query, transaction).getData());

        PlayerStats stats1 = new PlayerStats();
        stats1.setId("0");
        stats1.setLowScore(241);
        stats1.setOverallRating("Great");
        stats1.setRecordedDate(new Day(Date.valueOf("2019-07-11")));

        assertEquals(ImmutableList.of(stats1), results);
    }

    /**
     * Test filtering on an attribute that's not present in the query.
     *
     * @throws Exception exception
     */
    @Test
    public void testNotProjectedFilter() throws Exception {
        Query query = Query.builder()
                .source(playerStatsViewTable)
                .metricProjection(playerStatsViewTable.getMetricProjection("highScore"))
                .whereFilter(filterParser.parseFilterExpression("countryName=='United States'",
                        playerStatsViewType, false))
                .arguments(playerStatsViewTableArgs)
                .build();

        List<Object> results = toList(engine.executeQuery(query, transaction).getData());

        PlayerStatsView stats2 = new PlayerStatsView();
        stats2.setId("0");
        stats2.setHighScore(3147483647L);

        assertEquals(ImmutableList.of(stats2), results);
    }

    @Test
    public void testSortAggregatedMetric() throws Exception {
        Map<String, Sorting.SortOrder> sortMap = new TreeMap<>();
        sortMap.put("lowScore", Sorting.SortOrder.desc);

        Query query = Query.builder()
                .source(playerStatsTable)
                .dimensionProjection(playerStatsTable.getDimensionProjection("overallRating"))
                .metricProjection(playerStatsTable.getMetricProjection("lowScore"))
                .sorting(new SortingImpl(sortMap, PlayerStats.class, dictionary))
                .build();

        List<Object> results = toList(engine.executeQuery(query, transaction).getData());

        PlayerStats stats0 = new PlayerStats();
        stats0.setId("0");
        stats0.setLowScore(241);
        stats0.setOverallRating("Great");

        PlayerStats stats1 = new PlayerStats();
        stats1.setId("1");
        stats1.setLowScore(35);
        stats1.setOverallRating("Good");

        assertEquals(ImmutableList.of(stats0, stats1), results);
    }

    /**
     * Test sorting by dimension attribute which is not present in the query.
     *
     * @throws Exception exception
     */
    @Test
    public void testSortJoin() throws Exception {
        Map<String, Sorting.SortOrder> sortMap = new TreeMap<>();
        sortMap.put("playerName", Sorting.SortOrder.asc);

        Query query = Query.builder()
                .source(playerStatsTable)
                .metricProjection(playerStatsTable.getMetricProjection("lowScore"))
                .dimensionProjection(playerStatsTable.getDimensionProjection("overallRating"))
                .timeDimensionProjection(playerStatsTable.getTimeDimensionProjection("recordedDate"))
                .sorting(new SortingImpl(sortMap, PlayerStats.class, dictionary))
                .build();

        List<Object> results = toList(engine.executeQuery(query, transaction).getData());

        PlayerStats stats0 = new PlayerStats();
        stats0.setId("0");
        stats0.setLowScore(72);
        stats0.setOverallRating("Good");
        stats0.setRecordedDate(new Day(Date.valueOf("2019-07-13")));

        PlayerStats stats1 = new PlayerStats();
        stats1.setId("1");
        stats1.setLowScore(241);
        stats1.setOverallRating("Great");
        stats1.setRecordedDate(new Day(Date.valueOf("2019-07-11")));

        PlayerStats stats2 = new PlayerStats();
        stats2.setId("2");
        stats2.setLowScore(35);
        stats2.setOverallRating("Good");
        stats2.setRecordedDate(new Day(Date.valueOf("2019-07-12")));

        assertEquals(ImmutableList.of(stats0, stats1, stats2), results);
    }

    /**
     * Test pagination.
     *
     * @throws Exception exception
     */
    @Test
    public void testPagination() throws Exception {
        Query query = Query.builder()
                .source(playerStatsTable)
                .metricProjection(playerStatsTable.getMetricProjection("lowScore"))
                .dimensionProjection(playerStatsTable.getDimensionProjection("overallRating"))
                .timeDimensionProjection(playerStatsTable.getTimeDimensionProjection("recordedDate"))
                .pagination(new ImmutablePagination(0, 1, false, true))
                .build();

        QueryResult result = engine.executeQuery(query, transaction);
        List<Object> data = toList(result.getData());

        //Jon Doe,1234,72,Good,840,2019-07-12 00:00:00
        PlayerStats stats1 = new PlayerStats();
        stats1.setId("0");
        stats1.setLowScore(35);
        stats1.setOverallRating("Good");
        stats1.setRecordedDate(new Day(Date.valueOf("2019-07-12")));

        assertEquals(ImmutableList.of(stats1), data, "Returned record does not match");
        assertEquals(3, result.getPageTotals(), "Page totals does not match");
    }

    /**
     * Nested Queries with filter - Pagination
     * @throws Exception
     */
    @Test
    public void testPaginationWithFilter() throws Exception {
        Query query = Query.builder()
                .source(playerStatsTable)
                .metricProjection(playerStatsTable.getMetricProjection("dailyAverageScorePerPeriod"))
                .dimensionProjection(playerStatsTable.getDimensionProjection("overallRating"))
                .whereFilter(filterParser.parseFilterExpression("overallRating==Great", playerStatsType, false))
                .timeDimensionProjection(playerStatsTable.getTimeDimensionProjection("recordedDate"))
                .pagination(new ImmutablePagination(0, 1, false, true))
                .build();

        QueryResult result = engine.executeQuery(query, transaction);
        List<Object> data = toList(result.getData());

        //Jon Doe,1234,72,Good,840,2019-07-12 00:00:00
        PlayerStats stats1 = new PlayerStats();
        stats1.setId("0");
        stats1.setDailyAverageScorePerPeriod(3.147483647E9);
        stats1.setOverallRating("Great");
        stats1.setRecordedDate(new Day(Date.valueOf("2019-07-11")));

        assertEquals(ImmutableList.of(stats1), data, "Returned record does not match");
        assertEquals(1, result.getPageTotals(), "Page totals does not match");
    }

    /**
     * Test having clause integrates with group by clause.
     *
     * @throws Exception exception
     */
    @Test
    public void testHavingClause() throws Exception {
        Query query = Query.builder()
                .source(playerStatsTable)
                .metricProjection(playerStatsTable.getMetricProjection("highScore"))
                .dimensionProjection(playerStatsTable.getDimensionProjection("overallRating"))
                .havingFilter(filterParser.parseFilterExpression("highScore < 2400", playerStatsType, false))
                .build();

        List<Object> results = toList(engine.executeQuery(query, transaction).getData());

        // Only "Good" rating would have total high score less than 2400
        PlayerStats stats1 = new PlayerStats();
        stats1.setId("0");
        stats1.setOverallRating("Good");
        stats1.setHighScore(1234);

        assertEquals(ImmutableList.of(stats1), results);
    }

    /**
     * Test having clause integrates with group by clause and join.
     *
     * @throws Exception exception
     */
    @Test
    public void testHavingClauseJoin() throws Exception {
        Query query = Query.builder()
                .source(playerStatsTable)
                .metricProjection(playerStatsTable.getMetricProjection("highScore"))
                .dimensionProjection(playerStatsTable.getDimensionProjection("overallRating"))
                .dimensionProjection(playerStatsTable.getDimensionProjection("countryIsoCode"))
                .havingFilter(filterParser.parseFilterExpression("countryIsoCode==USA", playerStatsType, false))
                .build();

        List<Object> results = toList(engine.executeQuery(query, transaction).getData());

        PlayerStats stats0 = new PlayerStats();
        stats0.setId("1");
        stats0.setOverallRating("Great");
        stats0.setCountryIsoCode("USA");
        stats0.setHighScore(3147483647L);

        PlayerStats stats1 = new PlayerStats();
        stats1.setId("0");
        stats1.setOverallRating("Good");
        stats1.setCountryIsoCode("USA");
        stats1.setHighScore(1234);

        assertEquals(ImmutableList.of(stats1, stats0), results);
    }

    /**
     * Test sorting by two different columns-one metric and one dimension.
     *
     * @throws Exception exception
     */
    @Test
    public void testSortByMultipleColumns() throws Exception {
        Map<String, Sorting.SortOrder> sortMap = new TreeMap<>();
        sortMap.put("lowScore", Sorting.SortOrder.desc);
        sortMap.put("playerName", Sorting.SortOrder.asc);

        Query query = Query.builder()
                .source(playerStatsTable)
                .metricProjection(playerStatsTable.getMetricProjection("lowScore"))
                .dimensionProjection(playerStatsTable.getDimensionProjection("overallRating"))
                .timeDimensionProjection(playerStatsTable.getTimeDimensionProjection("recordedDate"))
                .sorting(new SortingImpl(sortMap, PlayerStats.class, dictionary))
                .build();

        List<Object> results = toList(engine.executeQuery(query, transaction).getData());

        PlayerStats stats0 = new PlayerStats();
        stats0.setId("0");
        stats0.setLowScore(241);
        stats0.setOverallRating("Great");
        stats0.setRecordedDate(new Day(Date.valueOf("2019-07-11")));

        PlayerStats stats1 = new PlayerStats();
        stats1.setId("1");
        stats1.setLowScore(72);
        stats1.setOverallRating("Good");
        stats1.setRecordedDate(new Day(Date.valueOf("2019-07-13")));

        PlayerStats stats2 = new PlayerStats();
        stats2.setId("2");
        stats2.setLowScore(35);
        stats2.setOverallRating("Good");
        stats2.setRecordedDate(new Day(Date.valueOf("2019-07-12")));

        assertEquals(ImmutableList.of(stats0, stats1, stats2), results);
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
                .dimensionProjection(playerStatsTable.getDimensionProjection("countryIsoCode"))
                .build();

        List<Object> results = toList(engine.executeQuery(query, transaction).getData());

        PlayerStats stats1 = new PlayerStats();
        stats1.setId("1");
        stats1.setHighScore(3147483647L);
        stats1.setCountryIsoCode("USA");

        PlayerStats stats2 = new PlayerStats();
        stats2.setId("0");
        stats2.setHighScore(1000);
        stats2.setCountryIsoCode("HKG");

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
                .whereFilter(filterParser.parseFilterExpression("countryIsoCode==USA", playerStatsType, false))
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

        assertEquals(ImmutableList.of(stats1, stats2), results);
    }

    /**
     * Test grouping by a dimension with a JoinTo annotation.
     *
     * @throws Exception exception
     */
    @Test
    public void testJoinToSort() throws Exception {
        Map<String, Sorting.SortOrder> sortMap = new TreeMap<>();
        sortMap.put("countryIsoCode", Sorting.SortOrder.asc);
        sortMap.put("highScore", Sorting.SortOrder.asc);

        Query query = Query.builder()
                .source(playerStatsTable)
                .metricProjection(playerStatsTable.getMetricProjection("highScore"))
                .dimensionProjection(playerStatsTable.getDimensionProjection("overallRating"))
                .dimensionProjection(playerStatsTable.getDimensionProjection("countryIsoCode"))
                .sorting(new SortingImpl(sortMap, PlayerStats.class, dictionary))
                .build();

        List<Object> results = toList(engine.executeQuery(query, transaction).getData());

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
        stats3.setHighScore(3147483647L);

        assertEquals(ImmutableList.of(stats1, stats2, stats3), results);
    }

    /**
     * Test month grain query.
     *
     * @throws Exception exception
     */
    @Test
    public void testTotalScoreByMonth() throws Exception {
        Query query = Query.builder()
                .source(playerStatsTable)
                .metricProjection(playerStatsTable.getMetricProjection("highScore"))
                .timeDimensionProjection(playerStatsTable.getTimeDimensionProjection("recordedDate"))
                .build();

        //Change for monthly column
        List<Object> results = toList(engine.executeQuery(query, transaction).getData());

        PlayerStats stats0 = new PlayerStats();
        stats0.setId("0");
        stats0.setHighScore(3147483647L);
        stats0.setRecordedDate(new Day(Date.valueOf("2019-07-11")));

        PlayerStats stats1 = new PlayerStats();
        stats1.setId("1");
        stats1.setHighScore(1234);
        stats1.setRecordedDate(new Day(Date.valueOf("2019-07-12")));

        PlayerStats stats2 = new PlayerStats();
        stats2.setId("2");
        stats2.setHighScore(1000);
        stats2.setRecordedDate(new Day(Date.valueOf("2019-07-13")));

        assertEquals(ImmutableList.of(stats0, stats1, stats2), results);

    }

    /**
     * Test filter by time dimension.
     *
     * @throws Exception exception
     */
    @Test
    public void testFilterByTemporalDimension() throws Exception {
        FilterPredicate predicate = new FilterPredicate(
                new Path(PlayerStats.class, dictionary, "recordedDate"),
                Operator.IN,
                Lists.newArrayList(new Day(Date.valueOf("2019-07-11"))));

        Query query = Query.builder()
                .source(playerStatsTable)
                .metricProjection(playerStatsTable.getMetricProjection("highScore"))
                .timeDimensionProjection(playerStatsTable.getTimeDimensionProjection("recordedDate"))
                .whereFilter(predicate)
                .build();

        List<Object> results = toList(engine.executeQuery(query, transaction).getData());

        PlayerStats stats0 = new PlayerStats();
        stats0.setId("0");
        stats0.setHighScore(3147483647L);
        stats0.setRecordedDate(new Day(Date.valueOf("2019-07-11")));

        assertEquals(ImmutableList.of(stats0), results);
    }

    @Test
    public void testAmbiguousFields() throws Exception {
        Map<String, Sorting.SortOrder> sortMap = new TreeMap<>();
        sortMap.put("lowScore", Sorting.SortOrder.asc);

        Query query = Query.builder()
                .source(playerStatsTable)
                .dimensionProjection(playerStatsTable.getDimensionProjection("playerName"))
                .dimensionProjection(playerStatsTable.getDimensionProjection("player2Name"))
                .metricProjection(playerStatsTable.getMetricProjection("lowScore"))
                .sorting(new SortingImpl(sortMap, PlayerStats.class, dictionary))
                .build();

        List<Object> results = toList(engine.executeQuery(query, transaction).getData());

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

        assertEquals(ImmutableList.of(stats0, stats1, stats2), results);
    }

    @Test
    public void testNullJoinToStringValue() throws Exception {
        Query query = Query.builder()
                .source(playerStatsTable)
                .metricProjection(playerStatsTable.getMetricProjection("highScore"))
                .dimensionProjection(playerStatsTable.getDimensionProjection("countryNickName"))
                .build();

        List<Object> results = toList(engine.executeQuery(query, transaction).getData());

        PlayerStats stats1 = new PlayerStats();
        stats1.setId("1");
        stats1.setHighScore(3147483647L);
        stats1.setCountryNickName("Uncle Sam");

        PlayerStats stats2 = new PlayerStats();
        stats2.setId("0");
        stats2.setHighScore(1000);
        stats2.setCountryNickName(null);

        assertEquals(ImmutableList.of(stats2, stats1), results);
    }

    @Test
    public void testNullJoinToIntValue() throws Exception {
        Query query = Query.builder()
                .source(playerStatsTable)
                .metricProjection(playerStatsTable.getMetricProjection("highScore"))
                .dimensionProjection(playerStatsTable.getDimensionProjection("countryUnSeats"))
                .build();

        List<Object> results = toList(engine.executeQuery(query, transaction).getData());

        PlayerStats stats1 = new PlayerStats();
        stats1.setId("1");
        stats1.setHighScore(3147483647L);
        stats1.setCountryUnSeats(1);

        PlayerStats stats2 = new PlayerStats();
        stats2.setId("0");
        stats2.setHighScore(1000);
        stats2.setCountryUnSeats(0);

        assertEquals(ImmutableList.of(stats2, stats1), results);
    }

    @Test
    public void testMultipleTimeGrains() throws Exception {
        Map<String, Argument> dayArguments = new HashMap<>();
        dayArguments.put("grain", Argument.builder().name("grain").value(TimeGrain.DAY).build());

        Map<String, Argument> monthArguments = new HashMap<>();
        monthArguments.put("grain", Argument.builder().name("grain").value(TimeGrain.MONTH).build());

        Map<String, Sorting.SortOrder> sortMap = new TreeMap<>();
        sortMap.put("highScore", Sorting.SortOrder.asc);

        Query query = Query.builder()
                .source(playerStatsTable)
                .metricProjection(playerStatsTable.getMetricProjection("highScore"))
                .timeDimensionProjection(
                        playerStatsTable.getTimeDimensionProjection("recordedDate", "byDay", dayArguments))
                .timeDimensionProjection(
                        playerStatsTable.getTimeDimensionProjection("recordedDate", "byMonth", monthArguments))
                .sorting(new SortingImpl(sortMap, PlayerStats.class, dictionary))
                .build();

        List<PlayerStats> results = toList(engine.executeQuery(query, transaction).getData());
        assertEquals(3, results.size());
        assertEquals(1000, results.get(0).getHighScore());
        assertEquals(new Day(Date.valueOf("2019-07-13")), results.get(0).fetch("byDay", null));
        assertEquals(new Month(Date.valueOf("2019-07-01")), results.get(0).fetch("byMonth", null));
        assertEquals(1234, results.get(1).getHighScore());
        assertEquals(new Day(Date.valueOf("2019-07-12")), results.get(1).fetch("byDay", null));
        assertEquals(new Month(Date.valueOf("2019-07-01")), results.get(1).fetch("byMonth", null));
        assertEquals(3147483647L, results.get(2).getHighScore());
        assertEquals(new Day(Date.valueOf("2019-07-11")), results.get(2).fetch("byDay", null));
        assertEquals(new Month(Date.valueOf("2019-07-01")), results.get(2).fetch("byMonth", null));
    }

    @Test
    public void testMultipleTimeGrainsFilteredByDayAlias() throws Exception {
        Map<String, Argument> dayArguments = new HashMap<>();
        dayArguments.put("grain", Argument.builder().name("grain").value(TimeGrain.DAY).build());

        Map<String, Argument> monthArguments = new HashMap<>();
        monthArguments.put("grain", Argument.builder().name("grain").value(TimeGrain.MONTH).build());

        Map<String, Sorting.SortOrder> sortMap = new TreeMap<>();
        sortMap.put("highScore", Sorting.SortOrder.asc);

        FilterPredicate predicate = new FilterPredicate(
                new Path(ClassType.of(PlayerStats.class), dictionary, "recordedDate", "byDay",
                        new HashSet<>(dayArguments.values())),
                Operator.IN,
                Lists.newArrayList(new Day(Date.valueOf("2019-07-11"))));

        Query query = Query.builder()
                .source(playerStatsTable)
                .metricProjection(playerStatsTable.getMetricProjection("highScore"))
                .whereFilter(predicate)
                .timeDimensionProjection(
                        playerStatsTable.getTimeDimensionProjection("recordedDate", "byDay", dayArguments))
                .timeDimensionProjection(
                        playerStatsTable.getTimeDimensionProjection("recordedDate", "byMonth", monthArguments))
                .sorting(new SortingImpl(sortMap, PlayerStats.class, dictionary))
                .build();

        List<PlayerStats> results = toList(engine.executeQuery(query, transaction).getData());
        assertEquals(1, results.size());
        assertEquals(3147483647L, results.get(0).getHighScore());
        assertEquals(new Day(Date.valueOf("2019-07-11")), results.get(0).fetch("byDay", null));
        assertEquals(new Month(Date.valueOf("2019-07-01")), results.get(0).fetch("byMonth", null));
    }

    @Test
    public void testMultipleTimeGrainsFilteredByMonthAlias() throws Exception {
        Map<String, Argument> dayArguments = new HashMap<>();
        dayArguments.put("grain", Argument.builder().name("grain").value(TimeGrain.DAY).build());

        Map<String, Argument> monthArguments = new HashMap<>();
        monthArguments.put("grain", Argument.builder().name("grain").value(TimeGrain.MONTH).build());

        Map<String, Sorting.SortOrder> sortMap = new TreeMap<>();
        sortMap.put("highScore", Sorting.SortOrder.asc);

        FilterPredicate predicate = new FilterPredicate(
                new Path(ClassType.of(PlayerStats.class), dictionary, "recordedDate", "byMonth",
                        new HashSet<>(monthArguments.values())),
                Operator.IN,
                Lists.newArrayList(new Day(Date.valueOf("2019-07-01"))));

        Query query = Query.builder()
                .source(playerStatsTable)
                .metricProjection(playerStatsTable.getMetricProjection("highScore"))
                .whereFilter(predicate)
                .timeDimensionProjection(
                        playerStatsTable.getTimeDimensionProjection("recordedDate", "byDay", dayArguments))
                .timeDimensionProjection(
                        playerStatsTable.getTimeDimensionProjection("recordedDate", "byMonth", monthArguments))
                .sorting(new SortingImpl(sortMap, PlayerStats.class, dictionary))
                .build();

        List<PlayerStats> results = toList(engine.executeQuery(query, transaction).getData());
        assertEquals(3, results.size());
        assertEquals(1000, results.get(0).getHighScore());
        assertEquals(new Day(Date.valueOf("2019-07-13")), results.get(0).fetch("byDay", null));
        assertEquals(new Month(Date.valueOf("2019-07-01")), results.get(0).fetch("byMonth", null));

        assertEquals(1234, results.get(1).getHighScore());
        assertEquals(new Day(Date.valueOf("2019-07-12")), results.get(1).fetch("byDay", null));
        assertEquals(new Month(Date.valueOf("2019-07-01")), results.get(1).fetch("byMonth", null));

        assertEquals(3147483647L, results.get(2).getHighScore());
        assertEquals(new Day(Date.valueOf("2019-07-11")), results.get(2).fetch("byDay", null));
        assertEquals(new Month(Date.valueOf("2019-07-01")), results.get(2).fetch("byMonth", null));
    }

    @Test
    public void testMultipleTimeGrainsSortedByDayAlias() throws Exception {
        Map<String, Argument> dayArguments = new HashMap<>();
        dayArguments.put("grain", Argument.builder().name("grain").value(TimeGrain.DAY).build());

        Map<String, Argument> monthArguments = new HashMap<>();
        monthArguments.put("grain", Argument.builder().name("grain").value(TimeGrain.MONTH).build());

        Map<String, Sorting.SortOrder> sortMap = new TreeMap<>();
        sortMap.put("byDay", Sorting.SortOrder.asc);

        Set<Attribute> sortAttributes = new HashSet<>(Arrays.asList(Attribute.builder()
                .type(TIME_TYPE)
                .name("recordedDate")
                .alias("byDay")
                .arguments(dayArguments.values())
                .build()));

        Query query = Query.builder()
                .source(playerStatsTable)
                .metricProjection(playerStatsTable.getMetricProjection("highScore"))
                .timeDimensionProjection(
                        playerStatsTable.getTimeDimensionProjection("recordedDate", "byDay", dayArguments))
                .timeDimensionProjection(
                        playerStatsTable.getTimeDimensionProjection("recordedDate", "byMonth", monthArguments))
                .sorting(new SortingImpl(sortMap, ClassType.of(PlayerStats.class), sortAttributes, dictionary))
                .build();

        List<PlayerStats> results = toList(engine.executeQuery(query, transaction).getData());
        assertEquals(3, results.size());
        assertEquals(3147483647L, results.get(0).getHighScore());
        assertEquals(new Day(Date.valueOf("2019-07-11")), results.get(0).fetch("byDay", null));
        assertEquals(new Month(Date.valueOf("2019-07-01")), results.get(0).fetch("byMonth", null));

        assertEquals(1234, results.get(1).getHighScore());
        assertEquals(new Day(Date.valueOf("2019-07-12")), results.get(1).fetch("byDay", null));
        assertEquals(new Month(Date.valueOf("2019-07-01")), results.get(1).fetch("byMonth", null));

        assertEquals(1000, results.get(2).getHighScore());
        assertEquals(new Day(Date.valueOf("2019-07-13")), results.get(2).fetch("byDay", null));
        assertEquals(new Month(Date.valueOf("2019-07-01")), results.get(2).fetch("byMonth", null));
    }

    @Test
    public void testMetricFormulaWithQueryPlan() throws Exception {

        Map<String, Argument> arguments = new HashMap<>();
        arguments.put("grain", Argument.builder().name("grain").value(TimeGrain.MONTH).build());

        Query query = Query.builder()
                .source(playerStatsTable)
                .metricProjection(playerStatsTable.getMetricProjection("dailyAverageScorePerPeriod"))
                .timeDimensionProjection(
                        playerStatsTable.getTimeDimensionProjection("recordedDate", arguments))
                .build();

        List<Object> results = toList(engine.executeQuery(query, transaction).getData());

        PlayerStats stats0 = new PlayerStats();
        stats0.setId("0");
        stats0.setDailyAverageScorePerPeriod(1.0491619603333334E9);
        stats0.setRecordedDate(new Month(Date.valueOf("2019-07-01")));

        assertEquals(ImmutableList.of(stats0), results);
    }

    @Test
    public void testInvalidTimeGrain() {
        Map<String, Argument> arguments = new HashMap<>();
        arguments.put("grain", Argument.builder().name("grain").value(TimeGrain.YEAR).build());


        assertThrows(InvalidParameterizedAttributeException.class,
                () -> playerStatsTable.getTimeDimensionProjection("recordedDate", arguments));
    }
}
