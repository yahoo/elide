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
 * This annotation takes a complete list of supported aggregations with the first as the default aggregation.
 * </ol>
 * The AggregationDataStore binds entities that have at least one {@link MetricAggregation} annotation.
 * <p>
 * Example:
 * <pre>
 * {@code
 * {@literal @}MetricAggregation(aggregations = {Max.class, Median.class})
 * }
 * </pre>
 * {@code Max} is the default aggregation in the example above.
 */
@Documented
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface MetricAggregation {

    /**
     * The complete list of supported aggregations with the first as the default aggregation.
     *
     * @return a comprehensive list of provided aggregations
     */
    Class<? extends Aggregation>[] aggregations();
}
