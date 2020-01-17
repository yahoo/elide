/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation;

import com.yahoo.elide.datastores.aggregation.annotation.MetricFormula;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLColumn;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that a field is computed via a {@link #expression() custom dimensino formula expression}, such as Calcite
 * SQL. This is similar to {@link MetricFormula}, except that dimension formula would be applied before aggregation.
 * <p>
 * Example: {@literal @}MetricFormula(expression = "IF({%1} >= 0, 'positive', 'negative')", references = {'ref1'}).
 *
 * Rules:
 * 1. The references used to replace '{%1}' and '{%2}' should be provided in the reference list.
 * 2. The provided references should only be other dimension field defined in the same class or join to a field defined
 *    in a dimension table.
 * 3. The reference list is 1-indexed.
 * 4. Each reference can be reused in the formula.
 * 5. Reference to same dimension field can be referred repeatedly using different indexes '{%#}'.
 * 6. Avoid cycle-reference.
 * 7. Sql expression can be carried when used with @JoinTo, the outer layer expression would be applied after the
 *    initial joined to sql expression.
 * 8. When a reference provided in the references list can't be found in the current table model, it would be treated
 *    as the physical column name. This also means, if a physical column name conflicts with a field name defined in the
 *    data model, the physical column name would be resolved as the field name and the generated expression can be
 *    wrong.
 * <p>
 *
 * {@code expression} can also be composite. During {@link SQLColumn} construction, it will substitute attribute names
 * in the provided expression with either:
 * <ul>
 *     <li> The column field name for that column in the query, or
 *     <li> a physical column name that doesn't conflict with any field name
 *     <li> Another {@link DimensionFormula} expression - recursively expanding expressions until every referenced
 *          field is not computed.
 * </ul>
 * For example, considering the following entity:
 * <pre>
 * {@code
 * public class FactTable {
 *     @DimensionFormula(
 *             expression = "CASE WHEN {%1} = 'Good' THEN 1 ELSE 2 END",
 *             references = {"overallRating"})
 *     public int getPlayerLevel() {
 *         return playerLevel;
 *     }
 *
 *     @JoinTo(path = "country.inUsa")
 *     public boolean isInUsa() {
 *         return inUsa;
 *     }
 *
 *     @DimensionFormula(
 *             expression = "CASE WHEN {%1} THEN 'true' ELSE 'false' END",
 *             references = {"country.inUsa"})
 *     public String getCountryIsInUsa() {
 *         return countryIsInUsa;
 *     }
 * }
 * }
 * </pre>
 * During {@link SQLColumn} construction, {@code countryIsInUsa} the provided expression will be substituted with
 * {@code country.inUsa}.
 */
@Documented
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface DimensionFormula {
    /**
     * The custom metric expression that represents this dimension formula.
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
