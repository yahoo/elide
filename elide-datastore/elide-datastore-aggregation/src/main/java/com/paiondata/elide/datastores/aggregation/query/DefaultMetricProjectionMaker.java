/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.datastores.aggregation.query;

import com.paiondata.elide.core.request.Argument;
import com.paiondata.elide.datastores.aggregation.metadata.models.Metric;
import com.paiondata.elide.datastores.aggregation.queryengines.sql.query.SQLMetricProjection;

import java.util.Map;

/**
 * Default maker which constructs a SQLMetricProjection.
 */
public class DefaultMetricProjectionMaker implements MetricProjectionMaker {
    @Override
    public MetricProjection make(Metric metric, String alias, Map<String, Argument> arguments) {
        return new SQLMetricProjection(metric, alias, arguments);
    }
}
