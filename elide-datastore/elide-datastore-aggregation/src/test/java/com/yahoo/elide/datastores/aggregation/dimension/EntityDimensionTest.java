/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.dimension;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.datastores.aggregation.annotation.CardinalitySize;
import com.yahoo.elide.datastores.aggregation.annotation.FriendlyName;
import com.yahoo.elide.datastores.aggregation.example.Country;
import com.yahoo.elide.datastores.aggregation.example.PlayerStats;
import com.yahoo.elide.datastores.aggregation.example.VideoGame;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collections;

import javax.persistence.Entity;

public class EntityDimensionTest {

    /**
     * A class for testing un-happy path on finding friendly name.
     */
    @Entity
    @Include(rootLevel = true)
    private static class Book {
        private Long id;
        private String title;
        private String subTitle;

        public Long getId() {
            return id;
        }

        public void setId(final Long id) {
            this.id = id;
        }

        @FriendlyName
        public String getTitle() {
            return title;
        }

        @FriendlyName
        public String getSubTitle() {
            return subTitle;
        }
    }

    private EntityDictionary entityDictionary;

    @BeforeMethod
    public void setupEntityDictionary() {
        entityDictionary = new EntityDictionary(Collections.emptyMap());
        entityDictionary.bindEntity(PlayerStats.class);
        entityDictionary.bindEntity(Country.class);
        entityDictionary.bindEntity(VideoGame.class);
        entityDictionary.bindEntity(Book.class);
    }

    @Test
    public void testHappyPathFriendlyNameScan() {
        // 1 field with @FriendlyName
        Assert.assertEquals(
                EntityDimension.getFriendlyNameField(PlayerStats.class, entityDictionary),
                "overallRating"
        );

        // no field with @FriendlyName
        Assert.assertEquals(
                EntityDimension.getFriendlyNameField(VideoGame.class, entityDictionary),
                "id"
        );
    }

    /**
     * Multiple {@link FriendlyName} annotations in entity is illegal.
     */
    @Test(expectedExceptions = IllegalStateException.class)
    public void testUnhappyPathFriendlyNameScan() {
        EntityDimension.getFriendlyNameField(Book.class, entityDictionary);
    }

    @Test
    public void testCardinalityScan() {
        // annotation on entity
        Assert.assertEquals(
                EntityDimension.getEstimatedCardinality("country", PlayerStats.class, entityDictionary),
                CardinalitySize.SMALL
        );

        // annotation on field
        Assert.assertEquals(
                EntityDimension.getEstimatedCardinality("overallRating", PlayerStats.class, entityDictionary),
                CardinalitySize.MEDIUM
        );

        // default is used
        Assert.assertEquals(
                EntityDimension.getEstimatedCardinality("recordedDate", PlayerStats.class, entityDictionary),
                EntityDimension.getDefaultCardinality()
        );
    }
}
