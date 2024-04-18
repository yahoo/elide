/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package example.metrics;

import com.paiondata.elide.core.request.Argument;
import com.paiondata.elide.datastores.aggregation.metadata.models.Metric;
import com.paiondata.elide.datastores.aggregation.query.MetricProjection;
import com.paiondata.elide.datastores.aggregation.query.MetricProjectionMaker;
import com.paiondata.elide.datastores.aggregation.queryengines.sql.query.SQLMetricProjection;

import java.util.Map;

/**
 * For testing HJSON metric projection maker configuration.
 */
public class SalesViewOrderMax implements MetricProjectionMaker {
    @Override
    public MetricProjection make(Metric metric, String alias, Map<String, Argument> arguments) {
        return SQLMetricProjection.builder()
                .alias(alias)
                .arguments(arguments)
                .name(metric.getName())
                .expression("MAX({{ $order_total }})")
                .valueType(metric.getValueType())
                .columnType(metric.getColumnType())
                .build();
    }
}
