/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata.models.metric;

import com.yahoo.elide.datastores.aggregation.metadata.models.MetricFunction;
import com.yahoo.elide.request.Argument;

import java.util.Map;

/**
 * Represents an invoked metric function with alias and arguments provided in user request.
 */
public interface MetricFunctionInvocation {
    Map<String, Argument> getArguments();
    MetricFunction getFunction();
    AggregatedField getAggregatedField();
    String getAlias();
}
