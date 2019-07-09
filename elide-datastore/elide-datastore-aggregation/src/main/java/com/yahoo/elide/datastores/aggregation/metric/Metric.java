/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metric;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.datastores.aggregation.annotation.MetricAggregation;
import com.yahoo.elide.datastores.aggregation.annotation.MetricComputation;

import java.io.Serializable;
import java.util.List;

/**
 * Elide's definition of metric.
 * <p>
 * {@link Metric}s are any entity field with a {@link MetricAggregation} or {@link MetricComputation}.
 */
@Include(type = "metrics")
public interface Metric extends Serializable {

    /**
     * Returns the name of the field representing this {@link Metric} object as a {@link String}.
     *
     * @return the name of the field representing this {@link Metric}.
     */
    String getName();

    /**
     * Returns a human-readable name (allowing spaces) of this {@link Metric} object as a {@link String}.
     *
     * @return a human-readable name (allowing spaces) of this {@link Metric}.
     */
    String getLongName();

    /**
     * Returns a short description explaining the meaning of this {@link Metric}.
     *
     * @return metric description
     */
    String getDescription();

    /**
     * Returns the entity field type of this {@link Metric}.
     *
     * @return metric data type
     */
    Class<?> getDataType();

    /**
     * Returns the complete list of supported aggregations with the first as the default aggregation.
     *
     * @return a comprehensive list of provided aggregations
     */
    List<Class<? extends Aggregation>> getAggregations();

    /**
     * Returns a JPQL expression that represents this metric computation logic.
     * <p>
     * For computed metrics, this will be the expanded expression. For metric aggregations, this will be the UDF
     * function called against the column name.
     *
     * @return a JPQL formula for computing this {@link Metric} or default aggregation UDF on a base/simple metric
     */
    String getMetricExpression();
}
