/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata.metric;

import com.yahoo.elide.datastores.aggregation.metadata.models.MetricFunction;
import com.yahoo.elide.request.Argument;

import com.google.common.base.Functions;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * An invoked metric function instance applied on an aggregated field with provided arguments to project the result
 * as the alias.
 */
public interface MetricFunctionInvocation {
    /**
     * Get all arguments provided for this metric function.
     *
     * @return request arguments
     */
    List<Argument> getArguments();

    /**
     * Get a name-argument map contains all arguments.
     *
     * @return argument map
     */
    default Map<String, Argument> getArgumentMap() {
        return getArguments().stream()
                .collect(Collectors.toMap(Argument::getName, Functions.identity()));
    }

    /**
     * Get argument for a specific name.
     *
     * @param argumentName argument name
     * @return an argument
     */
    Argument getArgument(String argumentName);

    /**
     * Get invoked metric function.
     *
     * @return metric function
     */
    MetricFunction getFunction();

    /**
     * Get full expression with provided arguments.
     *
     * @return function expression
     */
    default String getFunctionExpression() {
        return getFunction().constructExpression(getArgumentMap());
    }

    /**
     * Get alias of this invocation.
     *
     * @return alias
     */
    String getAlias();
}
