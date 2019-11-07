/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata.models.metric.functions;

import com.yahoo.elide.datastores.aggregation.metadata.models.metric.SimpleMetricFunctionImpl;

/**
 * Canned SUM metric function.
 */
public class Sum extends SimpleMetricFunctionImpl {
    public Sum() {
        super("Sum", "Sum", "Calculate sum of a metric column");
    }
}
