/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata.metric;

import com.yahoo.elide.datastores.aggregation.metadata.models.MetricFunction;

import java.util.Map;
import java.util.function.Function;

/**
 * Functions used to compute metrics.
 */
public abstract class MetricComputation extends MetricFunction {
    private final Map<String, String> argumentNameMap;

    @Override
    public Function<String, String> getArgumentNameMapper() {
        return argumentNameMap::get;
    }

    public MetricComputation(Map<String, String> argumentNameMap) {
        this.argumentNameMap = argumentNameMap;
    }
}
