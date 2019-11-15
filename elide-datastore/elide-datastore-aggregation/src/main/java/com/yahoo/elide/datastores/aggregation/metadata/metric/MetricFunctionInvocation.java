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
public interface MetricFunctionInvocation extends AggregatableField {
    /**
     * Get all arguments provided for this metric function.
     *
     * @return request arguments
     */
    List<Argument> getArguments();

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
     * Get all fields that is invoked in this metric.
     *
     * @return all aggregatable fields
     */
    List<AggregatableField> getAggregatables();

    /**
     * Get alias of this invocation.
     *
     * @return alias
     */
    String getAlias();

    /**
     * If another metric function is applied on this field, it should use alias to reference this invocation.
     *
     * @return reference to this invocation
     */
    @Override
    default String getName() {
        return getAlias();
    }
}
