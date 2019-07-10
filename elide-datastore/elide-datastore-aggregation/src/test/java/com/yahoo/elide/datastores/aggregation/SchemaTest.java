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
        Assert.assertTrue(playerStatsSchema.isBaseMetric("highScore"));
        Assert.assertFalse(videoGameSchema.isBaseMetric("timeSpentPerSession"));
    }

    @Test void testMetricCheck() {
        Assert.assertTrue(playerStatsSchema.isMetricField("highScore"));
        Assert.assertFalse(playerStatsSchema.isMetricField("country"));
    }

    @Test
    public void testGetDimension() {
        Assert.assertEquals(playerStatsSchema.getDimension("country").getCardinality(), CardinalitySize.LARGE);
    }

    @Test
    public void testGetMetric() {
        Assert.assertEquals(playerStatsSchema.getMetric("highScore").getMetricExpression(), "MAX(%s)");
    }
}
