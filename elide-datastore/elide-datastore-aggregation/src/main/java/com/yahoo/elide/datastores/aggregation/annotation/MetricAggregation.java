/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.annotation;

import com.yahoo.elide.datastores.aggregation.metric.Aggregation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the annotated field is a metric column.
 * <p>
 * This annotation takes two arguments:
 * <ol>
 *     <li> the default aggregation function to apply to the metric, and
 *     <li> the complete list of supported aggregations
 * </ol>
 * The AggregationDataStore binds entities that have at least one {@link MetricAggregation} annotation.
 * <p>
 * Example: {@literal @}MetricAggregation(default = Sum.class, aggregations = {Max.class, Median.class})
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MetricAggregation {

    /**
     * The default aggregation function to apply to the metric.
     *
     * @return default aggregation
     */
    Class<? extends Aggregation> defaultAggregation();

    /**
     * The complete list of supported aggregations.
     *
     * @return a comprehensive list of provided aggregations
     */
    Class<? extends Aggregation>[] aggregations();
}
