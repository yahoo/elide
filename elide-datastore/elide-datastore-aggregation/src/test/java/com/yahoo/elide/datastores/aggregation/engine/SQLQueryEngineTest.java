/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.engine;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.filter.dialect.RSQLFilterDialect;
import com.yahoo.elide.datastores.aggregation.Query;
import com.yahoo.elide.datastores.aggregation.QueryEngine;
import com.yahoo.elide.datastores.aggregation.Schema;
import com.yahoo.elide.datastores.aggregation.dimension.TimeDimension;
import com.yahoo.elide.datastores.aggregation.example.Country;
import com.yahoo.elide.datastores.aggregation.example.PlayerStats;
import com.yahoo.elide.datastores.aggregation.example.PlayerStatsView;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
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
        filterParser = new RSQLFilterDialect(dictionary);

        playerStatsSchema = new Schema(PlayerStats.class, dictionary);
        playerStatsViewSchema = new Schema(PlayerStatsView.class, dictionary);
    }

    @Test
    public void testFullTableLoad() throws Exception {
        EntityManager em = emf.createEntityManager();
        QueryEngine engine = new SQLQueryEngine(em, dictionary);

        Query query = Query.builder()
                .entityClass(PlayerStats.class)
                .metrics(playerStatsSchema.getMetrics())
                .groupDimension(playerStatsSchema.getDimension("overallRating"))
                .timeDimension((TimeDimension) playerStatsSchema.getDimension("recordedDate"))
                .build();

        List<Object> results = StreamSupport.stream(engine.executeQuery(query).spliterator(), false)
                .collect(Collectors.toList());

        //Jon Doe,1234,72,Good,840,2019-07-12 00:00:00
        PlayerStats stats1 = new PlayerStats();
        stats1.setLowScore(72);
        stats1.setHighScore(1234);
        stats1.setOverallRating("Good");
        stats1.setRecordedDate(Timestamp.valueOf("2019-07-12 00:00:00"));

        PlayerStats stats2 = new PlayerStats();
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
                .entityClass(PlayerStats.class)
                .metrics(playerStatsSchema.getMetrics())
                .groupDimension(playerStatsSchema.getDimension("overallRating"))
                .timeDimension((TimeDimension) playerStatsSchema.getDimension("recordedDate"))
                .whereFilter(filterParser.parseFilterExpression("overallRating==Great",
                        PlayerStats.class, false))
                .build();

        List<Object> results = StreamSupport.stream(engine.executeQuery(query).spliterator(), false)
                .collect(Collectors.toList());

        PlayerStats stats1 = new PlayerStats();
        stats1.setLowScore(241);
        stats1.setHighScore(2412);
        stats1.setOverallRating("Great");
        stats1.setRecordedDate(Timestamp.valueOf("2019-07-11 00:00:00"));

        Assert.assertEquals(results.size(), 1);
        Assert.assertEquals(results.get(0), stats1);
    }

    @Test
    public void testSubqueryLoad() throws Exception {
        EntityManager em = emf.createEntityManager();
        QueryEngine engine = new SQLQueryEngine(em, dictionary);

        Query query = Query.builder()
                .entityClass(PlayerStatsView.class)
                .metrics(playerStatsViewSchema.getMetrics())
                .build();

        List<Object> results = StreamSupport.stream(engine.executeQuery(query).spliterator(), false)
                .collect(Collectors.toList());

        PlayerStatsView stats2 = new PlayerStatsView();
        stats2.setHighScore(2412);

        Assert.assertEquals(results.size(), 1);
        Assert.assertEquals(results.get(0), stats2);
    }
}
