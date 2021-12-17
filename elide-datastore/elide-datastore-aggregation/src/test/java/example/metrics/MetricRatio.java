/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package example.metrics;

import com.yahoo.elide.core.exceptions.BadRequestException;
import com.yahoo.elide.core.request.Argument;
import com.yahoo.elide.datastores.aggregation.metadata.models.Metric;
import com.yahoo.elide.datastores.aggregation.query.MetricProjection;
import com.yahoo.elide.datastores.aggregation.query.MetricProjectionMaker;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.query.SQLMetricProjection;

import java.util.Map;

/**
 * Metric ratio cookbook example.
 */
public class MetricRatio implements MetricProjectionMaker {
    @Override
    public MetricProjection make(Metric metric, String alias, Map<String, Argument> arguments) {

        Argument numerator = arguments.get("numerator");
        Argument denominator = arguments.get("denominator");

        if (numerator == null || denominator == null) {
            throw new BadRequestException("'numerator' and 'denominator' arguments are required for "
                    + metric.getName());
        }

        return SQLMetricProjection.builder()
                .alias(alias)
                .arguments(arguments)
                .name(metric.getName())
                .expression("{{" + numerator.getValue() + "}} / {{" + denominator.getValue() + "}}")
                .valueType(metric.getValueType())
                .columnType(metric.getColumnType())
                .build();
    }
}
