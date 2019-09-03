/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.annotation;

import com.yahoo.elide.datastores.aggregation.schema.Schema;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that a field is computed via a {@link #expression() custom metric formula expression}, such as Calcite SQL.
 * <p>
 * Example: {@literal @}ComputedMetric(expression = '(fieldA * fieldB) / 100').The field names should match the Elide
 * data model field names.
 * <p>
 * {@code expression} can also be composite. During {@link Schema} construction, it will substitute attribute names in
 * the provided expression with either:
 * <ul>
 *     <li> The column alias for that column in the query, or
 *     <li> Another ComputedMetric expression - recursively expanding expressions until every referenced field is not
 *          computed.
 * </ul>
 * For example, considering the following entity:
 * <pre>
 * {@code
 * public class FactTable {
 *
 *     {@literal @}MetricAggregation(sum.class)
 *     Long sessions
 *
 *     {@literal @}MetricAggregation(sum.class)
 *     Long timeSpent
 *
 *     {@literal @}MetricComputation(expression = "timeSpent / sessions")
 *     Float timeSpentPerSession
 *
 *     {@literal @}MetricComputation(expression = "timeSpentPerSession / gameCount")
 *     Float timeSpentPerGame
 * }
 * }
 * </pre>
 * During {@link Schema} construction, {@code timeSpentPerSession} the provided expression will be substituted with
 * {@code timeSpent / sessions}.
 */
@Documented
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface MetricComputation {

    /**
     * The custom metric expression that represents this metric computation logic.
     *
     * @return metric formula
     */
    String expression();
}
