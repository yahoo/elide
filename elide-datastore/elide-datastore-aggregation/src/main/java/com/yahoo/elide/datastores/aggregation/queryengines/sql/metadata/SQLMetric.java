/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.datastores.aggregation.metadata.models.FunctionArgument;
import com.yahoo.elide.datastores.aggregation.metadata.models.Metric;
import com.yahoo.elide.datastores.aggregation.metadata.models.MetricFunction;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metric.SQLMetricFunction;

import java.util.Set;

/**
 * SQLMetric would contain {@link SQLMetricFunction} instead of {@link MetricFunction}.
 */
public class SQLMetric extends Metric {
    public SQLMetric(Class<?> tableClass, String fieldName, EntityDictionary dictionary) {
        super(tableClass, fieldName, dictionary);
    }

    @Override
    protected SQLMetricFunction constructMetricFunction(String id,
                                                     String longName,
                                                     String description,
                                                     String expression,
                                                     Set<FunctionArgument> arguments) {
        return new SQLMetricFunction(id, longName, description, expression, arguments);
    }
}
