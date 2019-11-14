/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata.metric;

import com.yahoo.elide.datastores.aggregation.metadata.models.Metric;

import lombok.Getter;

/**
 * A field in a physical/logical table that is actually aggregated by a metric function.
 * It can be either a metric field from a physical table or a field alias in a subquery.
 */
public class AggregatedField {
    @Getter
    private boolean isMetricField;

    @Getter
    private Metric metric;

    private String alias;

    public final String getFieldName() {
        return isMetricField ? metric.getName() : alias;
    }

    public AggregatedField(String alias) {
        this.isMetricField = false;
        this.alias = alias;
    }

    public AggregatedField(Metric metric) {
        this.isMetricField = true;
        this.metric = metric;
    }
}
