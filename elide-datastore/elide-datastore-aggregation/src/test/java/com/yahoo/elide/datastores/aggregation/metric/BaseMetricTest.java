/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metric;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class BaseMetricTest {

    private static final Metric SIMPLE_METRIC = new BaseMetric(
            "highScore",
            "awesome score",
            "very awesome score",
            long.class,
            Max.class,
            Arrays.asList(Max.class, Min.class)
    );

    private static final Metric COMPUTED_METRIC = new BaseMetric(
            "timeSpentPerGame",
            "Time Spent Per Game",
            "foo bar bat baz",
            Float.class,
            Max.class,
            Arrays.asList(Max.class, Min.class),
            "timeSpent / sessions / 100"
    );

    @Test
    public void testMetricAsCollectionElement() {
        Assert.assertEquals(SIMPLE_METRIC, SIMPLE_METRIC);
        Assert.assertEquals(COMPUTED_METRIC, COMPUTED_METRIC);
        Assert.assertNotEquals(SIMPLE_METRIC, COMPUTED_METRIC);
        Assert.assertNotEquals(SIMPLE_METRIC.hashCode(), COMPUTED_METRIC.hashCode());

        // different metrics should be separate elements in Set
        Set<Metric> set = new HashSet<>();
        set.add(SIMPLE_METRIC);

        Assert.assertEquals(set.size(), 1);

        set.add(SIMPLE_METRIC);
        Assert.assertEquals(set.size(), 1);

        set.add(COMPUTED_METRIC);
        Assert.assertEquals(set.size(), 2);
    }

    @Test
    public void testToString() {
        // simple metric
        Assert.assertEquals(
                SIMPLE_METRIC.toString(),
                "BaseMetric[name='highScore', longName='awesome score', description='very awesome score', dataType=long, defaultAggregation=Max, allAggregations=Max, Min, computedMetricExpression='N/A']"
        );

        // computed metric
        Assert.assertEquals(
                COMPUTED_METRIC.toString(),
                "BaseMetric[name='timeSpentPerGame', longName='Time Spent Per Game', description='foo bar bat baz', dataType=class java.lang.Float, defaultAggregation=Max, allAggregations=Max, Min, computedMetricExpression='timeSpent / sessions / 100']"
        );
    }
}
