/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.dimension;

import com.yahoo.elide.datastores.aggregation.annotation.CardinalitySize;
import com.yahoo.elide.datastores.aggregation.example.Country;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashSet;
import java.util.Set;

public class EntityDimensionTest {

    private static final Dimension ENTITY_DIMENSION = new EntityDimension(
            "country",
            "Country Domain Model",
            "A model that represents all information about a country",
            Country.class,
            CardinalitySize.SMALL,
            "name"
    );

    private static final Dimension DEGENERATE_DIMENSION = new DegenerateDimension(
            "overallRating",
            "Overall Rating",
            "How is this guy doing",
            String.class,
            CardinalitySize.SMALL,
            "overallRating",
            DefaultColumnType.FIELD
    );

    @Test
    public void testDimensionAsCollectionElement() {
        Assert.assertEquals(ENTITY_DIMENSION, ENTITY_DIMENSION);
        Assert.assertEquals(DEGENERATE_DIMENSION, DEGENERATE_DIMENSION);
        Assert.assertNotEquals(ENTITY_DIMENSION, DEGENERATE_DIMENSION);
        Assert.assertNotEquals(ENTITY_DIMENSION.hashCode(), DEGENERATE_DIMENSION.hashCode());

        // different dimensions should be separate elements in Set
        Set<Dimension> dimensions = new HashSet<>();
        dimensions.add(ENTITY_DIMENSION);

        Assert.assertEquals(dimensions.size(), 1);

        dimensions.add(ENTITY_DIMENSION);
        Assert.assertEquals(dimensions.size(), 1);

        dimensions.add(DEGENERATE_DIMENSION);
        Assert.assertEquals(dimensions.size(), 2);
    }

    @Test
    public void testToString() {
        // table dimension
        Assert.assertEquals(
                ENTITY_DIMENSION.toString(),
                "EntityDimension[name='country', longName='Country Domain Model', description='A model that represents all information about a country', dimensionType=TABLE, dataType=Country, cardinality=SMALL, friendlyName='name']"
        );

        // degenerate dimension
        Assert.assertEquals(
                DEGENERATE_DIMENSION.toString(),
                "EntityDimension[name='overallRating', longName='Overall Rating', description='How is this guy doing', dimensionType=DEGENERATE, dataType=String, cardinality=SMALL, friendlyName='overallRating', columnType=FIELD]"
        );
    }
}
