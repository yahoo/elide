/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.datastores.aggregation.query;

import com.paiondata.elide.core.request.Argument;
import com.paiondata.elide.datastores.aggregation.metadata.models.Metric;

import java.util.Map;

/**
 * Creates a metric projection for a given metric and client arguments.
 */
@FunctionalInterface
public interface MetricProjectionMaker {

    /**
     * Constructs a metric projection from a Metric, alias, and arguments.
     * @param metric the metric that is being projected.
     * @param alias the client assigned alias.
     * @param arguments The client provided arguments.
     * @return A new metric projection.
     */
    MetricProjection make(Metric metric, String alias, Map<String, Argument> arguments);
}
