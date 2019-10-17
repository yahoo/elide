/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.schema.dimension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.mock;

import com.yahoo.elide.datastores.aggregation.annotation.CardinalitySize;
import com.yahoo.elide.datastores.aggregation.annotation.Grain;
import com.yahoo.elide.datastores.aggregation.example.Country;
import com.yahoo.elide.datastores.aggregation.query.ProjectedDimension;
import com.yahoo.elide.datastores.aggregation.schema.Schema;
import com.yahoo.elide.datastores.aggregation.schema.dimension.impl.DegenerateDimension;
import com.yahoo.elide.datastores.aggregation.schema.dimension.impl.EntityDimension;
import com.yahoo.elide.datastores.aggregation.schema.dimension.impl.TimeDimension;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;

public class DimensionTest {

    private static final Schema MOCK_SCHEMA = mock(Schema.class);

    private static final DimensionColumn ENTITY_DIMENSION = new EntityDimension(
            MOCK_SCHEMA,
            "country",
            null,
            Country.class,
            CardinalitySize.SMALL,
            "name"
    );

    private static final DimensionColumn DEGENERATE_DIMENSION = new DegenerateDimension(
            MOCK_SCHEMA,
            "overallRating",
            null,
            String.class,
            CardinalitySize.SMALL,
            "overallRating",
            ColumnType.FIELD
    );

    private static final DimensionColumn TIME_DIMENSION = new TimeDimension(
            MOCK_SCHEMA,
            "recordedTime",
            null,
            Long.class,
            CardinalitySize.LARGE,
            "recordedTime",
            TimeZone.getTimeZone("PST"),
            new Grain[0]
    );

    @Test
    public void testDimensionAsCollectionElement() {
        assertEquals(ENTITY_DIMENSION, ENTITY_DIMENSION);
        assertEquals(DEGENERATE_DIMENSION, DEGENERATE_DIMENSION);
        assertNotEquals(DEGENERATE_DIMENSION, ENTITY_DIMENSION);
        assertNotEquals(DEGENERATE_DIMENSION.hashCode(), ENTITY_DIMENSION.hashCode());

        // different dimensions should be separate elements in Set
        Set<ProjectedDimension> projectedDimensions = new HashSet<>();
        projectedDimensions.add(ENTITY_DIMENSION);

        assertEquals(1, projectedDimensions.size());

        // a separate same object doesn't increase collection size
        ProjectedDimension sameEntityDimension = new EntityDimension(
                MOCK_SCHEMA,
                "country",
                null,
                Country.class,
                CardinalitySize.SMALL,
                "name"
        );
        assertEquals(ENTITY_DIMENSION, sameEntityDimension);
        projectedDimensions.add(sameEntityDimension);
        assertEquals(1, projectedDimensions.size());

        projectedDimensions.add(ENTITY_DIMENSION);
        assertEquals(1, projectedDimensions.size());

        projectedDimensions.add(DEGENERATE_DIMENSION);
        assertEquals(2, projectedDimensions.size());

        projectedDimensions.add(TIME_DIMENSION);
        assertEquals(3, projectedDimensions.size());
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
                "TimeDimension[timeZone=Pacific Standard Time, timeGrains=[], columnType=TEMPORAL, name='recordedTime', longName='recordedTime', description='recordedTime', dimensionType=DEGENERATE, dataType=class java.lang.Long, cardinality=LARGE, friendlyName='recordedTime']",
                TIME_DIMENSION.toString()
        );
    }
}
