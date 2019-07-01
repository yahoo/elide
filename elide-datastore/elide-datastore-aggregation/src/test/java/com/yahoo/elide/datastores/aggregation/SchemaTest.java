/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.datastores.aggregation.annotation.CardinalitySize;
import com.yahoo.elide.datastores.aggregation.example.Country;
import com.yahoo.elide.datastores.aggregation.example.PlayerStats;
import com.yahoo.elide.datastores.aggregation.example.VideoGame;
import com.yahoo.elide.datastores.aggregation.metric.Metric;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collections;

public class SchemaTest {

    private EntityDictionary entityDictionary;
    private Schema playerStatsSchema;
    private Schema videoGameSchema;

    @BeforeMethod
    public void setupEntityDictionary() {
        entityDictionary = new EntityDictionary(Collections.emptyMap());
        entityDictionary.bindEntity(PlayerStats.class);
        entityDictionary.bindEntity(Country.class);
        entityDictionary.bindEntity(VideoGame.class);

        playerStatsSchema = new Schema(PlayerStats.class, entityDictionary);
        videoGameSchema = new Schema(VideoGame.class, entityDictionary);
    }

    @Test
    public void testBaseMetricCheck() {
        Assert.assertTrue(playerStatsSchema.isBaseMetric("sessions", VideoGame.class));
        Assert.assertFalse(playerStatsSchema.isBaseMetric("timeSpentPerGame", VideoGame.class));
    }

    @Test void testMetricCheck() {
        Assert.assertTrue(playerStatsSchema.isMetricField("highScore", PlayerStats.class));
        Assert.assertFalse(playerStatsSchema.isMetricField("country", PlayerStats.class));
    }

    @Test
    public void testGetDimensionSize() {
        Assert.assertTrue(playerStatsSchema.getDimensionSize("country").isPresent());
        Assert.assertEquals(playerStatsSchema.getDimensionSize("country").get(), CardinalitySize.SMALL);
    }

    @Test
    public void testComputedMetricExpressionExpanssion() {
        // timeSpentPerSession
        Metric timeSpentPerSession = videoGameSchema.getMetrics().stream()
                .filter(metric -> "timeSpentPerSession".equals(metric.getName()))
                .findFirst()
                .get();

        Assert.assertEquals(
                timeSpentPerSession.getExpandedMetricExpression().get(),
                "<PREFIX>.timeSpent / <PREFIX>.rounds"
        );

        // timeSpentPerGame
        Metric timeSpentPerGame = videoGameSchema.getMetrics().stream()
                .filter(metric -> "timeSpentPerGame".equals(metric.getName()))
                .findFirst()
                .get();

        Assert.assertEquals(
                timeSpentPerGame.getExpandedMetricExpression().get(),
                "<PREFIX>.timeSpent / <PREFIX>.rounds / 100"
        );
    }
}
