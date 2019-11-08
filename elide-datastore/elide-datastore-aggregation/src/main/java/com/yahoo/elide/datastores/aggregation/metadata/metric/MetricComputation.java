/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata.metric;

import com.yahoo.elide.datastores.aggregation.metadata.models.MetricFunction;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Special Metric function that represents computation of other metrics. It contains a argumentNameMap to convert
 * arguments assigned to this metric to actual arguments for sub metric functions. e.g.
 * <pre>
 *     &#064;Metric(function = funWithArgs(a1, a2))
 *     private int metricA;
 *
 *     &#064;Metric(function = funWithArgs(b1, b2))
 *     private int metricB;
 *
 *     &#064;MetricComputation(expression = metricA(p1, p2) / metricB(p1, p3))
 *     private float ratio();
 * </pre>
 * Would map 'p1' to 'a1' and 'b1', 'p2' to 'a2' and 'p3' to 'b2'.
 */
public abstract class MetricComputation extends MetricFunction {
    private final Map<String, List<String>> argumentNameMap;

    @Override
    public Function<String, List<String>> getArgumentNameMapper() {
        return argumentNameMap::get;
    }

    public MetricComputation(Map<String, List<String>> argumentNameMap) {
        this.argumentNameMap = argumentNameMap;
    }
}
