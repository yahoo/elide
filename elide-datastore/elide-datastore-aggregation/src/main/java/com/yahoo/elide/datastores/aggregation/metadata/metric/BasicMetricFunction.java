/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata.metric;

import com.yahoo.elide.datastores.aggregation.metadata.models.FunctionArgument;
import com.yahoo.elide.datastores.aggregation.metadata.models.MetricFunction;
import com.yahoo.elide.request.Argument;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Basic implementation for Metric function.
 */
@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
public abstract class BasicMetricFunction extends MetricFunction {
    private String name;

    private String longName;

    private String description;

    private Set<FunctionArgument> arguments;

    protected BasicMetricFunction(String name, String longName, String description) {
        this(name, longName, description, Collections.emptySet());
    }

    @Override
    public MetricFunctionInvocation invoke(Map<String, Argument> arguments,
                                           List<AggregatableField> fields,
                                           String alias) {
        final MetricFunction function = this;
        return new MetricFunctionInvocation() {
            @Override
            public List<Argument> getArguments() {
                return new ArrayList<>(arguments.values());
            }

            @Override
            public Argument getArgument(String argumentName) {
                return arguments.get(argumentName);
            }

            @Override
            public MetricFunction getFunction() {
                return function;
            }

            @Override
            public List<AggregatableField> getAggregatables() {
                return fields;
            }

            @Override
            public String getAlias() {
                return alias;
            }
        };
    }
}
