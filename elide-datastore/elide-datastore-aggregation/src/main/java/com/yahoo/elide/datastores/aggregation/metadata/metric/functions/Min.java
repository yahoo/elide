/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata.metric.functions;

import com.yahoo.elide.datastores.aggregation.metadata.metric.MetricFunctionInvocation;
import com.yahoo.elide.datastores.aggregation.metadata.models.Metric;
import com.yahoo.elide.datastores.aggregation.metadata.models.MetricFunction;
import com.yahoo.elide.request.Argument;

import java.util.Collections;
import java.util.Set;

/**
 * Canned MIN metric function.
 */
public class Min extends MetricFunction {
    public Min() {
        super("Min", "Min", "Calculate min of a metric column", Collections.emptySet());
    }

    @Override
    public MetricFunctionInvocation invoke(String[] arguments, Metric metric, String alias) {
        final MetricFunction function = this;
        return new MetricFunctionInvocation() {
            @Override
            public MetricFunction getFunction() {
                return function;
            }

            @Override
            public Metric getMetric() {
                return metric;
            }

            @Override
            public String getAlias() {
                return alias;
            }

            @Override
            public Set<Argument> getExpressionArguments() {
                return Collections.emptySet();
            }

            @Override
            public Set<Argument> getAttributeArguments() {
                return Collections.emptySet();
            }
        };
    }
}
