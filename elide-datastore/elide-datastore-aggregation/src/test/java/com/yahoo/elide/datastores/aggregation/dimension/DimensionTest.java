/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.dimension;

import com.yahoo.elide.datastores.aggregation.annotation.CardinalitySize;
import com.yahoo.elide.datastores.aggregation.example.Country;
import com.yahoo.elide.datastores.aggregation.time.DefaultTimeGrain;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;

public class DimensionTest {

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

    private static final Dimension TIME_DIMENSION = new TimeDimension(
            "recordedTime",
            "Recorded Time",
            "This is recorded time in UNIX timestemmp",
            Long.class,
            CardinalitySize.LARGE,
            "recordedTime",
            TimeZone.getDefault(),
            Collections.singleton(DefaultTimeGrain.SECOND)
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

        dimensions.add(TIME_DIMENSION);
        Assert.assertEquals(dimensions.size(), 3);
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
                "DegenerateDimension[columnType=FIELD, name='overallRating', longName='Overall Rating', description='How is this guy doing', dimensionType=DEGENERATE, dataType=class java.lang.String, cardinality=SMALL, friendlyName='overallRating']"
        );

        Assert.assertEquals(
                TIME_DIMENSION.toString(),
                "TimeDimension[timeZone=Pacific Standard Time, timeGrain=[second], columnType=TEMPORAL, name='recordedTime', longName='Recorded Time', description='This is recorded time in UNIX timestemmp', dimensionType=DEGENERATE, dataType=class java.lang.Long, cardinality=LARGE, friendlyName='recordedTime']"
        );
    }
}
