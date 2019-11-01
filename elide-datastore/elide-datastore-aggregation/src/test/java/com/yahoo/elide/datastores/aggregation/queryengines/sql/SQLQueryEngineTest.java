/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql;

import static com.yahoo.elide.datastores.aggregation.metadata.models.MetricFunction.getAggregationFunction;
import static com.yahoo.elide.datastores.aggregation.queryengines.sql.SubselectTest.toProjection;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.yahoo.elide.core.filter.dialect.RSQLFilterDialect;
import com.yahoo.elide.core.pagination.Pagination;
import com.yahoo.elide.core.sort.Sorting;
import com.yahoo.elide.datastores.aggregation.AggregationDictionary;
import com.yahoo.elide.datastores.aggregation.QueryEngine;
import com.yahoo.elide.datastores.aggregation.example.Country;
import com.yahoo.elide.datastores.aggregation.example.Player;
import com.yahoo.elide.datastores.aggregation.example.PlayerStats;
import com.yahoo.elide.datastores.aggregation.example.PlayerStatsView;
import com.yahoo.elide.datastores.aggregation.example.SubCountry;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import com.yahoo.elide.datastores.aggregation.metadata.enums.Aggregation;
import com.yahoo.elide.datastores.aggregation.metadata.models.Table;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.datastores.aggregation.time.TimeGrain;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

public class SQLQueryEngineTest {
    private static EntityManagerFactory emf;
    private static Table playerStatsTable;
    private static Table playerStatsViewTable;
    private static AggregationDictionary dictionary;
    private static RSQLFilterDialect filterParser;
    private static QueryEngine engine;

    private static final Country HONG_KONG = new Country();
    private static final Country USA = new Country();

    @BeforeAll
    public static void init() {
        emf = Persistence.createEntityManagerFactory("aggregationStore");
        dictionary = new AggregationDictionary(new HashMap<>());
        dictionary.bindEntity(PlayerStats.class);
        dictionary.bindEntity(PlayerStatsView.class);
        dictionary.bindEntity(Country.class);
        dictionary.bindEntity(SubCountry.class);
        dictionary.bindEntity(Player.class);
        filterParser = new RSQLFilterDialect(dictionary);

        playerStatsTable = new Table(PlayerStats.class, dictionary);
        playerStatsViewTable = new Table(PlayerStatsView.class, dictionary);

        MetaDataStore metaDataStore = new MetaDataStore();
        metaDataStore.storeMetaData(dictionary);
        engine = new SQLQueryEngine(emf, dictionary, metaDataStore);

        HONG_KONG.setIsoCode("HKG");
        HONG_KONG.setName("Hong Kong");
        HONG_KONG.setId("344");

        USA.setIsoCode("USA");
        USA.setName("United States");
        USA.setId("840");
    }

    /**
     * Test loading all three records from the table.
     */
    @Test
    public void testFullTableLoad() {
        Query query = Query.builder()
                .table(playerStatsTable)
                .metric(playerStatsTable.getMetric("lowScore"), getAggregationFunction(Aggregation.SUM))
                .metric(playerStatsTable.getMetric("highScore"), getAggregationFunction(Aggregation.SUM))
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
                .table(playerStatsTable)
                .metric(playerStatsTable.getMetric("lowScore"), getAggregationFunction(Aggregation.SUM))
                .metric(playerStatsTable.getMetric("highScore"), getAggregationFunction(Aggregation.SUM))
                .groupDimension(toProjection(playerStatsTable.getDimension("overallRating")))
                .timeDimension(toProjection(playerStatsTable.getTimeDimension("recordedDate"), TimeGrain.DAY))
                .whereFilter(filterParser.parseFilterExpression("overallRating==Great",
                        PlayerStats.class, false))
                .build();

        List<Object> results = StreamSupport.stream(engine.executeQuery(query).spliterator(), false)
                .collect(Collectors.toList());

        PlayerStats stats1 = new PlayerStats();
        stats1.setId("0");
        stats1.setLowScore(241);
        stats1.setHighScore(2412);
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
                .table(playerStatsTable)
                .metric(playerStatsTable.getMetric("lowScore"), getAggregationFunction(Aggregation.SUM))
                .metric(playerStatsTable.getMetric("highScore"), getAggregationFunction(Aggregation.SUM))
                .groupDimension(toProjection(playerStatsTable.getDimension("overallRating")))
                .groupDimension(toProjection(playerStatsTable.getDimension("country")))
                .timeDimension(toProjection(playerStatsTable.getTimeDimension("recordedDate"), TimeGrain.DAY))
                .whereFilter(filterParser.parseFilterExpression("country.name=='United States'",
                        PlayerStats.class, false))
                .build();

        List<Object> results = StreamSupport.stream(engine.executeQuery(query).spliterator(), false)
                .collect(Collectors.toList());

        PlayerStats usa0 = new PlayerStats();
        usa0.setId("0");
        usa0.setLowScore(241);
        usa0.setHighScore(2412);
        usa0.setOverallRating("Great");
        usa0.setCountry(USA);
        usa0.setRecordedDate(Timestamp.valueOf("2019-07-11 00:00:00"));

        PlayerStats usa1 = new PlayerStats();
        usa1.setId("1");
        usa1.setLowScore(35);
        usa1.setHighScore(1234);
        usa1.setOverallRating("Good");
        usa1.setCountry(USA);
        usa1.setRecordedDate(Timestamp.valueOf("2019-07-12 00:00:00"));

        assertEquals(2, results.size());
        assertEquals(usa0, results.get(0));
        assertEquals(usa1, results.get(1));

        // test join
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
                .table(playerStatsViewTable)
                .metric(playerStatsViewTable.getMetric("highScore"), getAggregationFunction(Aggregation.SUM))
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
     *
     * @throws Exception exception
     */
    @Test
    public void testSubqueryLoad() throws Exception {
        Query query = Query.builder()
                .table(playerStatsViewTable)
                .metric(playerStatsViewTable.getMetric("highScore"), getAggregationFunction(Aggregation.SUM))
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
                .table(playerStatsTable)
                .metric(playerStatsTable.getMetric("lowScore"), getAggregationFunction(Aggregation.SUM))
                .groupDimension(toProjection(playerStatsTable.getDimension("overallRating")))
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
                .table(playerStatsTable)
                .metric(playerStatsTable.getMetric("lowScore"), getAggregationFunction(Aggregation.SUM))
                .metric(playerStatsTable.getMetric("highScore"), getAggregationFunction(Aggregation.SUM))
                .groupDimension(toProjection(playerStatsTable.getDimension("overallRating")))
                .timeDimension(toProjection(playerStatsTable.getTimeDimension("recordedDate"), TimeGrain.DAY))
                .pagination(pagination)
                .build();

        List<Object> results = StreamSupport.stream(engine.executeQuery(query).spliterator(), false)
                .collect(Collectors.toList());

        //Jon Doe,1234,72,Good,840,2019-07-12 00:00:00
        PlayerStats stats1 = new PlayerStats();
        stats1.setId("0");
        stats1.setLowScore(35);
        stats1.setHighScore(1234);
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
                .metric(playerStatsTable.getMetric("highScore"), getAggregationFunction(Aggregation.SUM))
                .groupDimension(toProjection(playerStatsTable.getDimension("overallRating")))
                .havingFilter(filterParser.parseFilterExpression("highScore < 2400",
                        PlayerStats.class, false))
                .build();

        List<Object> results = StreamSupport.stream(engine.executeQuery(query).spliterator(), false)
                .collect(Collectors.toList());

        // Only "Good" rating would have total high score less than 2400
        PlayerStats stats1 = new PlayerStats();
        stats1.setId("0");
        stats1.setOverallRating("Good");
        stats1.setHighScore(2234);

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
                .metric(playerStatsTable.getMetric("highScore"), getAggregationFunction(Aggregation.SUM))
                .groupDimension(toProjection(playerStatsTable.getDimension("overallRating")))
                .groupDimension(toProjection(playerStatsTable.getDimension("countryIsoCode")))
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
    public void testTheEverythingQuery() throws Exception {
        Map<String, Sorting.SortOrder> sortMap = new TreeMap<>();
        sortMap.put("player.name", Sorting.SortOrder.asc);

        Query query = Query.builder()
                .table(playerStatsViewTable)
                .metric(playerStatsViewTable.getMetric("highScore"), getAggregationFunction(Aggregation.SUM))
                .groupDimension(toProjection(playerStatsViewTable.getDimension("countryName")))
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
                .table(playerStatsTable)
                .metric(playerStatsTable.getMetric("lowScore"), getAggregationFunction(Aggregation.SUM))
                .groupDimension(toProjection(playerStatsTable.getDimension("overallRating")))
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

        Query query = Query.builder()
                .table(playerStatsTable)
                .metric(playerStatsTable.getMetric("lowScore"), getAggregationFunction(Aggregation.SUM))
                .metric(playerStatsTable.getMetric("highScore"), getAggregationFunction(Aggregation.SUM))
                .groupDimension(toProjection(playerStatsTable.getDimension("overallRating")))
                .groupDimension(toProjection(playerStatsTable.getDimension("country")))
                .timeDimension(toProjection(playerStatsTable.getTimeDimension("recordedDate"), TimeGrain.DAY))
                .sorting(new Sorting(sortMap))
                .build();

        List<Object> results = StreamSupport.stream(engine.executeQuery(query).spliterator(), false)
                .collect(Collectors.toList());

        PlayerStats usa0 = new PlayerStats();
        usa0.setId("0");
        usa0.setLowScore(241);
        usa0.setHighScore(2412);
        usa0.setOverallRating("Great");
        usa0.setCountry(USA);
        usa0.setRecordedDate(Timestamp.valueOf("2019-07-11 00:00:00"));

        PlayerStats usa1 = new PlayerStats();
        usa1.setId("1");
        usa1.setLowScore(35);
        usa1.setHighScore(1234);
        usa1.setOverallRating("Good");
        usa1.setCountry(USA);
        usa1.setRecordedDate(Timestamp.valueOf("2019-07-12 00:00:00"));

        PlayerStats hk2 = new PlayerStats();
        hk2.setId("2");
        hk2.setLowScore(72);
        hk2.setHighScore(1000);
        hk2.setOverallRating("Good");
        hk2.setCountry(HONG_KONG);
        hk2.setRecordedDate(Timestamp.valueOf("2019-07-13 00:00:00"));

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
     *
     * @throws Exception exception
     */
    @Test
    public void testJoinToGroupBy() throws Exception {
        Query query = Query.builder()
                .table(playerStatsTable)
                .metric(playerStatsTable.getMetric("highScore"), getAggregationFunction(Aggregation.SUM))
                .groupDimension(toProjection(playerStatsTable.getDimension("countryIsoCode")))
                .build();

        List<Object> results = StreamSupport.stream(engine.executeQuery(query).spliterator(), false)
                .collect(Collectors.toList());

        PlayerStats stats1 = new PlayerStats();
        stats1.setId("0");
        stats1.setHighScore(3646);
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
                .metric(playerStatsTable.getMetric("highScore"), getAggregationFunction(Aggregation.SUM))
                .groupDimension(toProjection(playerStatsTable.getDimension("overallRating")))
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
     *
     * @throws Exception exception
     */
    @Test
    public void testJoinToSort() throws Exception {
        Map<String, Sorting.SortOrder> sortMap = new TreeMap<>();
        sortMap.put("countryIsoCode", Sorting.SortOrder.asc);

        Query query = Query.builder()
                .table(playerStatsTable)
                .metric(playerStatsTable.getMetric("highScore"), getAggregationFunction(Aggregation.SUM))
                .groupDimension(toProjection(playerStatsTable.getDimension("overallRating")))
                .groupDimension(toProjection(playerStatsTable.getDimension("country")))
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

    //TODO - Add Invalid Request Tests
}
