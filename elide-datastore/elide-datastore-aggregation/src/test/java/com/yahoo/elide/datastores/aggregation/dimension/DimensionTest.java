/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.dimension;

import static org.mockito.Mockito.mock;

import com.yahoo.elide.datastores.aggregation.annotation.CardinalitySize;
import com.yahoo.elide.datastores.aggregation.example.Country;
import com.yahoo.elide.datastores.aggregation.schema.Schema;
import com.yahoo.elide.datastores.aggregation.time.TimeGrain;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;

public class DimensionTest {

    private static final Schema MOCK_SCHEMA = mock(Schema.class);

    private static final Dimension ENTITY_DIMENSION = new EntityDimension(
            MOCK_SCHEMA,
            "country",
            null,
            Country.class,
            CardinalitySize.SMALL,
            "name"
    );

    private static final Dimension DEGENERATE_DIMENSION = new DegenerateDimension(
            MOCK_SCHEMA,
            "overallRating",
            null,
            String.class,
            CardinalitySize.SMALL,
            "overallRating",
            ColumnType.FIELD
    );

    private static final Dimension TIME_DIMENSION = new TimeDimension(
            MOCK_SCHEMA,
            "recordedTime",
            null,
            Long.class,
            CardinalitySize.LARGE,
            "recordedTime",
            TimeZone.getTimeZone("PST"),
            TimeGrain.DAY
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

        // a separate same object doesn't increase collection size
        Dimension sameEntityDimension = new EntityDimension(
                MOCK_SCHEMA,
                "country",
                null,
                Country.class,
                CardinalitySize.SMALL,
                "name"
        );
        Assert.assertEquals(sameEntityDimension, ENTITY_DIMENSION);
        dimensions.add(sameEntityDimension);
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
                "EntityDimension[name='country', longName='country', description='country', dimensionType=ENTITY, dataType=Country, cardinality=SMALL, friendlyName='name']"
        );

        // degenerate dimension
        Assert.assertEquals(
                DEGENERATE_DIMENSION.toString(),
                "DegenerateDimension[columnType=FIELD, name='overallRating', longName='overallRating', description='overallRating', dimensionType=DEGENERATE, dataType=String, cardinality=SMALL, friendlyName='overallRating']"
        );

        Assert.assertEquals(
                TIME_DIMENSION.toString(),
                "TimeDimension[timeZone=Pacific Standard Time, timeGrain=DAY, columnType=TEMPORAL, name='recordedTime', longName='recordedTime', description='recordedTime', dimensionType=DEGENERATE, dataType=class java.lang.Long, cardinality=LARGE, friendlyName='recordedTime']"
        );
    }
}
