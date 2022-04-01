/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.custom;

import com.yahoo.elide.core.request.Argument;
import com.yahoo.elide.datastores.aggregation.metadata.models.Metric;
import com.yahoo.elide.datastores.aggregation.query.MetricProjection;
import com.yahoo.elide.datastores.aggregation.query.MetricProjectionMaker;

import java.util.Map;

/**
 * Factory method for custom metric DailyAverageScorePerPeriod.
 */
public class DailyAverageScorePerPeriodMaker implements MetricProjectionMaker {
    @Override
    public MetricProjection make(Metric metric, String alias, Map<String, Argument> arguments) {
        return new DailyAverageScorePerPeriod(metric, alias, arguments);
    }
}
