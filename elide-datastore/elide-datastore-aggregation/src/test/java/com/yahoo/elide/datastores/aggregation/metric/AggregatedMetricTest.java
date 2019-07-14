/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metric;

import static org.mockito.Mockito.mock;

import com.yahoo.elide.datastores.aggregation.Schema;
import org.testng.Assert;
import org.testng.annotations.Test;

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
        Assert.assertEquals(SIMPLE_METRIC_1, SIMPLE_METRIC_1);
        Assert.assertEquals(SIMPLE_METRIC_2, SIMPLE_METRIC_2);
        Assert.assertNotEquals(SIMPLE_METRIC_1, SIMPLE_METRIC_2);
        Assert.assertNotEquals(SIMPLE_METRIC_1.hashCode(), SIMPLE_METRIC_2.hashCode());

        // different metrics should be separate elements in Set
        Set<Metric> set = new HashSet<>();
        set.add(SIMPLE_METRIC_1);

        Assert.assertEquals(set.size(), 1);

        // a separate same object doesn't increase collection size
        Metric sameMetric = new AggregatedMetric(
                MOCK_SCHEMA,
                "highScore",
                null,
                long.class,
                Collections.singletonList(Max.class)
        );
        Assert.assertEquals(sameMetric, SIMPLE_METRIC_1);
        set.add(sameMetric);
        Assert.assertEquals(set.size(), 1);

        set.add(SIMPLE_METRIC_1);
        Assert.assertEquals(set.size(), 1);

        set.add(SIMPLE_METRIC_2);
        Assert.assertEquals(set.size(), 2);
    }

    @Test
    public void testToString() {
        // simple metric
        Assert.assertEquals(
                SIMPLE_METRIC_1.toString(),
                "AggregatedMetric[name='highScore', longName='highScore', description='highScore', dataType=long, aggregations=Max]"
        );

        // computed metric
        Assert.assertEquals(
                SIMPLE_METRIC_2.toString(),
                "AggregatedMetric[name='timeSpentPerGame', longName='timeSpentPerGame', description='timeSpentPerGame', dataType=class java.lang.Float, aggregations=Max]"
        );
    }
}
