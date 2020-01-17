/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.annotation;

import com.yahoo.elide.datastores.aggregation.metadata.models.Metric;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that a field is computed via a {@link #expression() custom metric formula expression}, such as Calcite SQL.
 * Metric formula is resolved as a new aggregation function, so it won't convert the value to a different format before
 * or after aggregation.
 * <p>
 * Example: {@literal @}MetricFormula(expression = '({%1} * {%2}) / 100', references = {'ref1', 'ref2'}).
 *
 * Rules:
 * 1. The references used to replace '{%1}' and '{%2}' should be provided in the reference list.
 * 2. The provided references should only be other metric field defined in the same class.
 * 3. The reference list is 1-indexed.
 * 4. Each reference can be reused in the formula.
 * 5. Reference to same metric field can be referred repeatedly using different indexes '{%#}'.
 * 6. Avoid cycle-reference.
 * 7. Can't apply formula to metric functions that don't have single literal expression, such as YearOverYear.
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
 *     {@literal @}MetricAggregation(sum.class)
 *     Long sessions
 *
 *     {@literal @}MetricAggregation(sum.class)
 *     Long timeSpent
 *
 *     {@literal @}MetricComputation(expression = "{%1} / {%2}", references = {"timeSpent", "sessions"})
 *     Float timeSpentPerSession
 *
 *     {@literal @}MetricComputation(expression = "{%1} / 100", references = {"timeSpentPerSession"})
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
     * The custom metric expression that represents this metric formula.
     *
     * @return metric formula
     */
    String expression();

    /**
     * References to use in the formula.
     *
     * @return references
     */
    String[] references() default {};
}
