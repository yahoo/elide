/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.datastores.aggregation.annotation.CardinalitySize;
import com.yahoo.elide.datastores.aggregation.example.Country;
import com.yahoo.elide.datastores.aggregation.example.PlayerStats;
import com.yahoo.elide.datastores.aggregation.example.VideoGame;
import com.yahoo.elide.datastores.aggregation.metric.Max;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.OneToMany;

public class SchemaTest {

    /**
     * Used to test un-happy path of dimension construction on ToMany relationship.
     */
    @Entity
    @Include(rootLevel = true)
    private static class Player {
        private Long id;
        private Set<VideoGame> favoriteGames;

        public Long getId() {
            return id;
        }

        public void setId(final Long id) {
            this.id = id;
        }

        @OneToMany
        public Set<VideoGame> getFavoriteGames() {
            return favoriteGames;
        }

        public void setFavoriteGames(final Set<VideoGame> favoriteGames) {
            this.favoriteGames = favoriteGames;
        }
    }

    private EntityDictionary entityDictionary;
    private Schema playerStatsSchema;

    @BeforeMethod
    public void setupEntityDictionary() {
        entityDictionary = new EntityDictionary(Collections.emptyMap());
        entityDictionary.bindEntity(PlayerStats.class);
        entityDictionary.bindEntity(Country.class);
        entityDictionary.bindEntity(VideoGame.class);
        entityDictionary.bindEntity(Player.class);

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
                "MAX(%s)"
        );
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testParameterizedDimension() {
        new Schema(Player.class, entityDictionary);
    }
}
