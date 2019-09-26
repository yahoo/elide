/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.engine;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.filter.dialect.RSQLFilterDialect;
import com.yahoo.elide.core.pagination.Pagination;
import com.yahoo.elide.core.sort.Sorting;
import com.yahoo.elide.datastores.aggregation.Query;
import com.yahoo.elide.datastores.aggregation.QueryEngine;
import com.yahoo.elide.datastores.aggregation.dimension.TimeDimension;
import com.yahoo.elide.datastores.aggregation.engine.schema.SQLSchema;
import com.yahoo.elide.datastores.aggregation.example.Country;
import com.yahoo.elide.datastores.aggregation.example.Player;
import com.yahoo.elide.datastores.aggregation.example.PlayerStats;
import com.yahoo.elide.datastores.aggregation.example.PlayerStatsView;
import com.yahoo.elide.datastores.aggregation.metric.Sum;
import com.yahoo.elide.datastores.aggregation.schema.Schema;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

public class SQLQueryEngineTest {

    private EntityManagerFactory emf;

    private Schema playerStatsSchema;
    private Schema playerStatsViewSchema;
    private EntityDictionary dictionary;
    private RSQLFilterDialect filterParser;

    public SQLQueryEngineTest() {
        emf = Persistence.createEntityManagerFactory("aggregationStore");
        dictionary = new EntityDictionary(new HashMap<>());
        dictionary.bindEntity(PlayerStats.class);
        dictionary.bindEntity(PlayerStatsView.class);
        dictionary.bindEntity(Country.class);
        dictionary.bindEntity(Player.class);
        filterParser = new RSQLFilterDialect(dictionary);

        playerStatsSchema = new SQLSchema(PlayerStats.class, dictionary);
        playerStatsViewSchema = new SQLSchema(PlayerStatsView.class, dictionary);
    }

    @Test
    public void testFullTableLoad() throws Exception {
        EntityManager em = emf.createEntityManager();
        QueryEngine engine = new SQLQueryEngine(em, dictionary);

        Query query = Query.builder()
                .schema(playerStatsSchema)
                .metric(playerStatsSchema.getMetric("lowScore"), Sum.class)
                .metric(playerStatsSchema.getMetric("highScore"), Sum.class)
                .groupDimension(playerStatsSchema.getDimension("overallRating"))
                .timeDimension((TimeDimension) playerStatsSchema.getDimension("recordedDate"))
                .build();

        List<Object> results = StreamSupport.stream(engine.executeQuery(query).spliterator(), false)
                .collect(Collectors.toList());

        //Jon Doe,1234,72,Good,840,2019-07-12 00:00:00
        PlayerStats stats1 = new PlayerStats();
        stats1.setId("0");
        stats1.setLowScore(72);
        stats1.setHighScore(1234);
        stats1.setOverallRating("Good");
        stats1.setRecordedDate(Timestamp.valueOf("2019-07-12 00:00:00"));

        PlayerStats stats2 = new PlayerStats();
        stats2.setId("1");
        stats2.setLowScore(241);
        stats2.setHighScore(2412);
        stats2.setOverallRating("Great");
        stats2.setRecordedDate(Timestamp.valueOf("2019-07-11 00:00:00"));

        Assert.assertEquals(results.size(), 2);
        Assert.assertEquals(results.get(0), stats1);
        Assert.assertEquals(results.get(1), stats2);
    }

    @Test
    public void testDegenerateDimensionFilter() throws Exception {
        EntityManager em = emf.createEntityManager();
        QueryEngine engine = new SQLQueryEngine(em, dictionary);

        Query query = Query.builder()
                .schema(playerStatsSchema)
                .metric(playerStatsSchema.getMetric("lowScore"), Sum.class)
                .metric(playerStatsSchema.getMetric("highScore"), Sum.class)
                .groupDimension(playerStatsSchema.getDimension("overallRating"))
                .timeDimension((TimeDimension) playerStatsSchema.getDimension("recordedDate"))
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

        Assert.assertEquals(results.size(), 1);
        Assert.assertEquals(results.get(0), stats1);
    }

    @Test
    public void testFilterJoin() throws Exception {
        EntityManager em = emf.createEntityManager();
        QueryEngine engine = new SQLQueryEngine(em, dictionary);

        Query query = Query.builder()
                .schema(playerStatsSchema)
                .metric(playerStatsSchema.getMetric("lowScore"), Sum.class)
                .metric(playerStatsSchema.getMetric("highScore"), Sum.class)
                .groupDimension(playerStatsSchema.getDimension("overallRating"))
                .groupDimension(playerStatsSchema.getDimension("country"))
                .timeDimension((TimeDimension) playerStatsSchema.getDimension("recordedDate"))
                .whereFilter(filterParser.parseFilterExpression("country.name=='United States'",
                        PlayerStats.class, false))
                .build();

        List<Object> results = StreamSupport.stream(engine.executeQuery(query).spliterator(), false)
                .collect(Collectors.toList());

        Country expectedCountry = new Country();
        expectedCountry.setId("840");
        expectedCountry.setIsoCode("USA");
        expectedCountry.setName("United States");


        PlayerStats stats1 = new PlayerStats();
        stats1.setId("0");
        stats1.setLowScore(241);
        stats1.setHighScore(2412);
        stats1.setOverallRating("Great");
        stats1.setCountry(expectedCountry);
        stats1.setRecordedDate(Timestamp.valueOf("2019-07-11 00:00:00"));

        PlayerStats stats2 = new PlayerStats();
        stats2.setId("1");
        stats2.setLowScore(72);
        stats2.setHighScore(1234);
        stats2.setOverallRating("Good");
        stats2.setCountry(expectedCountry);
        stats2.setRecordedDate(Timestamp.valueOf("2019-07-12 00:00:00"));

        Assert.assertEquals(results.size(), 2);
        Assert.assertEquals(results.get(0), stats1);
        Assert.assertEquals(results.get(1), stats2);

        // test join
        PlayerStats actualStats1 = (PlayerStats) results.get(0);
        Assert.assertNotNull(actualStats1.getCountry());
    }

    @Test
    public void testSubqueryFilterJoin() throws Exception {
        EntityManager em = emf.createEntityManager();
        QueryEngine engine = new SQLQueryEngine(em, dictionary);

        Query query = Query.builder()
                .schema(playerStatsViewSchema)
                .metric(playerStatsViewSchema.getMetric("highScore"), Sum.class)
                .whereFilter(filterParser.parseFilterExpression("player.name=='Jane Doe'",
                        PlayerStatsView.class, false))
                .build();

        List<Object> results = StreamSupport.stream(engine.executeQuery(query).spliterator(), false)
                .collect(Collectors.toList());

        PlayerStatsView stats2 = new PlayerStatsView();
        stats2.setId("0");
        stats2.setHighScore(2412);

        Assert.assertEquals(results.size(), 1);
        Assert.assertEquals(results.get(0), stats2);
    }

    @Test
    public void testSubqueryLoad() throws Exception {
        EntityManager em = emf.createEntityManager();
        QueryEngine engine = new SQLQueryEngine(em, dictionary);

        Query query = Query.builder()
                .schema(playerStatsViewSchema)
                .metric(playerStatsViewSchema.getMetric("highScore"), Sum.class)
                .build();

        List<Object> results = StreamSupport.stream(engine.executeQuery(query).spliterator(), false)
                .collect(Collectors.toList());

        PlayerStatsView stats2 = new PlayerStatsView();
        stats2.setId("0");
        stats2.setHighScore(2412);

        Assert.assertEquals(results.size(), 1);
        Assert.assertEquals(results.get(0), stats2);
    }

    @Test
    public void testSortJoin() throws Exception {
        EntityManager em = emf.createEntityManager();
        QueryEngine engine = new SQLQueryEngine(em, dictionary);

        Map<String, Sorting.SortOrder> sortMap = new TreeMap<>();
        sortMap.put("player.name", Sorting.SortOrder.asc);

        Query query = Query.builder()
                .schema(playerStatsSchema)
                .metric(playerStatsSchema.getMetric("lowScore"), Sum.class)
                .groupDimension(playerStatsSchema.getDimension("overallRating"))
                .timeDimension((TimeDimension) playerStatsSchema.getDimension("recordedDate"))
                .sorting(new Sorting(sortMap))
                .build();

        List<Object> results = StreamSupport.stream(engine.executeQuery(query).spliterator(), false)
                .collect(Collectors.toList());

        PlayerStats stats1 = new PlayerStats();
        stats1.setId("0");
        stats1.setLowScore(241);
        stats1.setOverallRating("Great");
        stats1.setRecordedDate(Timestamp.valueOf("2019-07-11 00:00:00"));

        PlayerStats stats2 = new PlayerStats();
        stats2.setId("1");
        stats2.setLowScore(72);
        stats2.setOverallRating("Good");
        stats2.setRecordedDate(Timestamp.valueOf("2019-07-12 00:00:00"));

        Assert.assertEquals(results.size(), 2);
        Assert.assertEquals(results.get(0), stats1);
        Assert.assertEquals(results.get(1), stats2);
    }

    @Test
    public void testPagination() throws Exception {
        EntityManager em = emf.createEntityManager();
        QueryEngine engine = new SQLQueryEngine(em, dictionary);

        Pagination pagination = Pagination.fromOffsetAndLimit(1, 0, true);

        Query query = Query.builder()
                .schema(playerStatsSchema)
                .metric(playerStatsSchema.getMetric("lowScore"), Sum.class)
                .metric(playerStatsSchema.getMetric("highScore"), Sum.class)
                .groupDimension(playerStatsSchema.getDimension("overallRating"))
                .timeDimension((TimeDimension) playerStatsSchema.getDimension("recordedDate"))
                .pagination(pagination)
                .build();

        List<Object> results = StreamSupport.stream(engine.executeQuery(query).spliterator(), false)
                .collect(Collectors.toList());

        //Jon Doe,1234,72,Good,840,2019-07-12 00:00:00
        PlayerStats stats1 = new PlayerStats();
        stats1.setId("0");
        stats1.setLowScore(72);
        stats1.setHighScore(1234);
        stats1.setOverallRating("Good");
        stats1.setRecordedDate(Timestamp.valueOf("2019-07-12 00:00:00"));

        Assert.assertEquals(results.size(), 1, "Number of records returned does not match");
        Assert.assertEquals(results.get(0), stats1, "Returned record does not match");
        Assert.assertEquals(pagination.getPageTotals(), 2, "Page totals does not match");
    }

    @Test
    public void testHavingClause() throws Exception {
        EntityManager em = emf.createEntityManager();
        QueryEngine engine = new SQLQueryEngine(em, dictionary);

        Query query = Query.builder()
                .schema(playerStatsSchema)
                .metric(playerStatsSchema.getMetric("highScore"), Sum.class)
                .havingFilter(filterParser.parseFilterExpression("highScore > 300",
                        PlayerStats.class, false))
                .build();

        List<Object> results = StreamSupport.stream(engine.executeQuery(query).spliterator(), false)
                .collect(Collectors.toList());

        //Jon Doe,1234,72,Good,840,2019-07-12 00:00:00
        PlayerStats stats1 = new PlayerStats();
        stats1.setId("0");
        stats1.setHighScore(3646);

        Assert.assertEquals(results.size(), 1);
        Assert.assertEquals(results.get(0), stats1);
    }

    @Test
    public void testTheEverythingQuery() throws Exception {
        EntityManager em = emf.createEntityManager();
        QueryEngine engine = new SQLQueryEngine(em, dictionary);

        Map<String, Sorting.SortOrder> sortMap = new TreeMap<>();
        sortMap.put("player.name", Sorting.SortOrder.asc);

        Query query = Query.builder()
                .schema(playerStatsViewSchema)
                .metric(playerStatsViewSchema.getMetric("highScore"), Sum.class)
                .groupDimension(playerStatsViewSchema.getDimension("countryName"))
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


        Assert.assertEquals(results.size(), 1);
        Assert.assertEquals(results.get(0), stats2);
    }

    @Test
    public void testSortByMultipleColumns() throws Exception {
        EntityManager em = emf.createEntityManager();
        QueryEngine engine = new SQLQueryEngine(em, dictionary);

        Map<String, Sorting.SortOrder> sortMap = new TreeMap<>();
        sortMap.put("lowScore", Sorting.SortOrder.desc);
        sortMap.put("player.name", Sorting.SortOrder.asc);

        Query query = Query.builder()
                .schema(playerStatsSchema)
                .metric(playerStatsSchema.getMetric("lowScore"), Sum.class)
                .groupDimension(playerStatsSchema.getDimension("overallRating"))
                .timeDimension((TimeDimension) playerStatsSchema.getDimension("recordedDate"))
                .sorting(new Sorting(sortMap))
                .build();

        List<Object> results = StreamSupport.stream(engine.executeQuery(query).spliterator(), false)
                .collect(Collectors.toList());

        PlayerStats stats1 = new PlayerStats();
        stats1.setId("0");
        stats1.setLowScore(241);
        stats1.setOverallRating("Great");
        stats1.setRecordedDate(Timestamp.valueOf("2019-07-11 00:00:00"));

        PlayerStats stats2 = new PlayerStats();
        stats2.setId("1");
        stats2.setLowScore(72);
        stats2.setOverallRating("Good");
        stats2.setRecordedDate(Timestamp.valueOf("2019-07-12 00:00:00"));

        Assert.assertEquals(results.size(), 2);
        Assert.assertEquals(results.get(0), stats1);
        Assert.assertEquals(results.get(1), stats2);
    }
}
