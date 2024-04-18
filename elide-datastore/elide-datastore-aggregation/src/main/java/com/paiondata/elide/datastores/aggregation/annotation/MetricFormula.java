/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.datastores.aggregation.annotation;

import com.paiondata.elide.datastores.aggregation.metadata.models.Metric;
import com.paiondata.elide.datastores.aggregation.query.DefaultMetricProjectionMaker;
import com.paiondata.elide.datastores.aggregation.query.MetricProjectionMaker;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that a field is computed via a {@link #value() custom metric formula expression}, such as Calcite SQL.
 * Metric formula is resolved as a new aggregation function, so it won't convert the value to a different format before
 * or after aggregation.
 * <p>
 * Example: {@literal @}MetricFormula("({{field1}} * {{field2}}) / 100").
 * <p>
 *
 * Rules:
 * 1. The provided references should only be other logical metric field defined in the same class or a physical column
 *    in current physical table.
 * 2. Avoid cycle-reference.
 * 3. Can't apply formula to metric functions that don't have single literal expression, such as YearOverYear.
 * <p>
 * {@code expression} can also be composite. During {@link Metric} construction, it will substitute attribute names in
 * the provided expression with either:
 * <ul>
 *     <li> The column field name for that column in the query, or
 *     <li> Another {@link MetricFormula} expression - recursively expanding expressions until every referenced
 *          field is not computed.
 * </ul>
 * For example, considering the following entity:
 * <pre>
 * {@code
 * public class FactTable {
 *
 *     {@literal @}MetricFormula("SUM({{sessions}})")
 *     Long sessions
 *
 *     {@literal @}MetricFormula("SUM({{timeSpent}})")
 *     Long timeSpent
 *
 *     {@literal @}MetricFormula("{{timeSpent}} / {{sessions}}")
 *     Float timeSpentPerSession
 *
 *     {@literal @}MetricFormula("{{timeSpentPerSession}} / 100")
 *     Float timeSpentPerGame
 * }
 * }
 * </pre>
 * During {@link Metric} construction, {@code timeSpentPerSession} the provided expression will be substituted with
 * {@code timeSpent / sessions}.
 */
@Documented
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface MetricFormula {
    /**
     * The custom metric expression that represents this metric computation logic.
     *
     * @return metric formula
     */
    String value() default "";

    /**
     * Function which constructs a projection for this given metric.
     * @return metric maker class.
     */
    Class<? extends MetricProjectionMaker> maker() default DefaultMetricProjectionMaker.class;

    /**
     * The arguments accepted by this table.
     * @return arguments for the metric
     */
    ArgumentDefinition[] arguments() default {};
}
