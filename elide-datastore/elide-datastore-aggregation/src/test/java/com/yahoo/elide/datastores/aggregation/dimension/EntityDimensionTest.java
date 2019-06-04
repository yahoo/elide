/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.dimension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.datastores.aggregation.annotation.CardinalitySize;
import com.yahoo.elide.datastores.aggregation.annotation.FriendlyName;
import com.yahoo.elide.datastores.aggregation.dimension.impl.EntityDimension;
import com.yahoo.elide.datastores.aggregation.example.Country;
import com.yahoo.elide.datastores.aggregation.example.PlayerStats;
import com.yahoo.elide.datastores.aggregation.example.VideoGame;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import javax.persistence.Entity;

public class EntityDimensionTest {
    private static EntityDictionary entityDictionary;

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

    @BeforeAll
    public static void setupEntityDictionary() {
        entityDictionary = new EntityDictionary(Collections.emptyMap());
        entityDictionary.bindEntity(PlayerStats.class);
        entityDictionary.bindEntity(Country.class);
        entityDictionary.bindEntity(VideoGame.class);
        entityDictionary.bindEntity(Book.class);
    }

    @Test
    public void testHappyPathFriendlyNameScan() {
        // 1 field with @FriendlyName
        assertEquals(
                "overallRating",
                EntityDimension.getFriendlyNameField(PlayerStats.class, entityDictionary)
        );

        // no field with @FriendlyName
        assertEquals(
                "id",
                EntityDimension.getFriendlyNameField(VideoGame.class, entityDictionary)
        );
    }

    /**
     * Multiple {@link FriendlyName} annotations in entity is illegal.
     */
    @Test
    public void testUnhappyPathFriendlyNameScan() {
        assertThrows(
                IllegalStateException.class,
                () -> EntityDimension.getFriendlyNameField(Book.class, entityDictionary));
    }

    @Test
    public void testCardinalityScan() {
        // annotation on entity
        assertEquals(
                CardinalitySize.SMALL,
                EntityDimension.getEstimatedCardinality("country", PlayerStats.class, entityDictionary)
        );

        // annotation on field
        assertEquals(
                CardinalitySize.MEDIUM,
                EntityDimension.getEstimatedCardinality("overallRating", PlayerStats.class, entityDictionary)
        );

        // default is used
        assertEquals(
                EntityDimension.getDefaultCardinality(),
                EntityDimension.getEstimatedCardinality("recordedDate", PlayerStats.class, entityDictionary)
        );
    }
}
