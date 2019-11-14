/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata.metric;

import com.yahoo.elide.datastores.aggregation.metadata.models.MetricFunction;
import com.yahoo.elide.request.Argument;

import java.util.List;

/**
 * An invoked metric function instance applied on an aggregated field with provided arguments to project the result
 * as the alias.
 */
public interface MetricFunctionInvocation {
    List<Argument> getArguments();

    Argument getArgument(String argumentName);

    MetricFunction getFunction();

    AggregatedField getAggregatedField();

    String getAlias();
}
