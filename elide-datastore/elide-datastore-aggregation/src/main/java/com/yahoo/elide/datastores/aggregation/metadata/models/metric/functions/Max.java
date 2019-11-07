/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata.models.metric.functions;

import com.yahoo.elide.datastores.aggregation.metadata.models.metric.SimpleMetricFunctionImpl;

/**
 * Canned MAX metric function.
 */
public class Max extends SimpleMetricFunctionImpl {
    public Max() {
        super("Max", "Max", "Calculate max of a metric column");
    }
}
