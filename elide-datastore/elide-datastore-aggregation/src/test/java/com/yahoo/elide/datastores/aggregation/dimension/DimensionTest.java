/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.dimension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.mock;

import com.yahoo.elide.datastores.aggregation.annotation.CardinalitySize;
import com.yahoo.elide.datastores.aggregation.dimension.impl.DegenerateDimension;
import com.yahoo.elide.datastores.aggregation.dimension.impl.EntityDimension;
import com.yahoo.elide.datastores.aggregation.dimension.impl.TimeDimension;
import com.yahoo.elide.datastores.aggregation.example.Country;
import com.yahoo.elide.datastores.aggregation.schema.Schema;
import com.yahoo.elide.datastores.aggregation.time.TimeGrain;
import org.junit.jupiter.api.Test;

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
        assertEquals(ENTITY_DIMENSION, ENTITY_DIMENSION);
        assertEquals(DEGENERATE_DIMENSION, DEGENERATE_DIMENSION);
        assertNotEquals(DEGENERATE_DIMENSION, ENTITY_DIMENSION);
        assertNotEquals(DEGENERATE_DIMENSION.hashCode(), ENTITY_DIMENSION.hashCode());

        // different dimensions should be separate elements in Set
        Set<Dimension> dimensions = new HashSet<>();
        dimensions.add(ENTITY_DIMENSION);

        assertEquals(1, dimensions.size());

        // a separate same object doesn't increase collection size
        Dimension sameEntityDimension = new EntityDimension(
                MOCK_SCHEMA,
                "country",
                null,
                Country.class,
                CardinalitySize.SMALL,
                "name"
        );
        assertEquals(ENTITY_DIMENSION, sameEntityDimension);
        dimensions.add(sameEntityDimension);
        assertEquals(1, dimensions.size());

        dimensions.add(ENTITY_DIMENSION);
        assertEquals(1, dimensions.size());

        dimensions.add(DEGENERATE_DIMENSION);
        assertEquals(2, dimensions.size());

        dimensions.add(TIME_DIMENSION);
        assertEquals(3, dimensions.size());
    }

    @Test
    public void testToString() {
        // table dimension
        assertEquals(
                "EntityDimension[name='country', longName='country', description='country', dimensionType=ENTITY, dataType=Country, cardinality=SMALL, friendlyName='name']",
                ENTITY_DIMENSION.toString()
        );

        // degenerate dimension
        assertEquals(
                "DegenerateDimension[columnType=FIELD, name='overallRating', longName='overallRating', description='overallRating', dimensionType=DEGENERATE, dataType=String, cardinality=SMALL, friendlyName='overallRating']",
                DEGENERATE_DIMENSION.toString()
        );

        assertEquals(
                "TimeDimension[timeZone=Pacific Standard Time, timeGrain=DAY, columnType=TEMPORAL, name='recordedTime', longName='recordedTime', description='recordedTime', dimensionType=DEGENERATE, dataType=class java.lang.Long, cardinality=LARGE, friendlyName='recordedTime']",
                TIME_DIMENSION.toString()
        );
    }
}
