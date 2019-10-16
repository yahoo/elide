/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.schema.metric;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.mock;

import com.yahoo.elide.datastores.aggregation.schema.Schema;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class AggregatedMetricTest {

    private static final Schema MOCK_SCHEMA = mock(Schema.class);

    private static final Metric SIMPLE_METRIC_1 = new AggregatedMetric(
            MOCK_SCHEMA,
            "highScore",
            null,
            long.class,
            Collections.singletonList(Max.class)
    );

    private static final Metric SIMPLE_METRIC_2 = new AggregatedMetric(
            MOCK_SCHEMA,
            "timeSpentPerGame",
            null,
            Float.class,
            Collections.singletonList(Max.class)
    );

    @Test
    public void testMetricAsCollectionElement() {
        assertEquals(SIMPLE_METRIC_1, SIMPLE_METRIC_1);
        assertEquals(SIMPLE_METRIC_2, SIMPLE_METRIC_2);
        assertNotEquals(SIMPLE_METRIC_1, SIMPLE_METRIC_2);
        assertNotEquals(SIMPLE_METRIC_1.hashCode(), SIMPLE_METRIC_2.hashCode());

        // different metrics should be separate elements in Set
        Set<Metric> set = new HashSet<>();
        set.add(SIMPLE_METRIC_1);

        assertEquals(1, set.size());

        // a separate same object doesn't increase collection size
        Metric sameMetric = new AggregatedMetric(
                MOCK_SCHEMA,
                "highScore",
                null,
                long.class,
                Collections.singletonList(Max.class)
        );
        assertEquals(SIMPLE_METRIC_1, sameMetric);
        set.add(sameMetric);
        assertEquals(1, set.size());

        set.add(SIMPLE_METRIC_1);
        assertEquals(1, set.size());

        set.add(SIMPLE_METRIC_2);
        assertEquals(2, set.size());
    }

    @Test
    public void testToString() {
        // simple metric
        assertEquals(
                "AggregatedMetric[name='highScore', longName='highScore', description='highScore', dataType=long, aggregations=Max]",
                SIMPLE_METRIC_1.toString()
        );

        // computed metric
        assertEquals(
                "AggregatedMetric[name='timeSpentPerGame', longName='timeSpentPerGame', description='timeSpentPerGame', dataType=class java.lang.Float, aggregations=Max]",
                SIMPLE_METRIC_2.toString()
        );
    }
}
