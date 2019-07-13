/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.datastores.aggregation.annotation.CardinalitySize;
import com.yahoo.elide.datastores.aggregation.example.Country;
import com.yahoo.elide.datastores.aggregation.example.Player;
import com.yahoo.elide.datastores.aggregation.example.PlayerStats;
import com.yahoo.elide.datastores.aggregation.example.VideoGame;
import com.yahoo.elide.datastores.aggregation.metric.Max;
import com.yahoo.elide.datastores.aggregation.schema.Schema;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.Optional;

public class SchemaTest {

    private EntityDictionary entityDictionary;
    private Schema playerStatsSchema;

    @BeforeMethod
    public void setupEntityDictionary() {
        entityDictionary = new EntityDictionary(Collections.emptyMap());
        entityDictionary.bindEntity(Country.class);
        entityDictionary.bindEntity(VideoGame.class);
        entityDictionary.bindEntity(Player.class);
        entityDictionary.bindEntity(PlayerStats.class);

        playerStatsSchema = new Schema(PlayerStats.class, entityDictionary);
    }

    @Test void testMetricCheck() {
        Assert.assertTrue(playerStatsSchema.isMetricField("highScore"));
        Assert.assertFalse(playerStatsSchema.isMetricField("country"));
    }

    @Test
    public void testGetDimension() {
        Assert.assertEquals(playerStatsSchema.getDimension("country").getCardinality(), CardinalitySize.SMALL);
    }

    @Test
    public void testGetMetric() {
        Assert.assertEquals(
                playerStatsSchema.getMetric("highScore").getMetricExpression(Optional.of(Max.class)),
                "MAX(com_yahoo_elide_datastores_aggregation_example_PlayerStats.highScore)"
        );
    }
}
