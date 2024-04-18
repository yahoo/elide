/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.datastores.aggregation.annotation;

import com.paiondata.elide.datastores.aggregation.metadata.models.Dimension;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that a field is computed via a {@link #value()} custom dimension formula expression}, such as Calcite
 * SQL. This is similar to {@link MetricFormula}, except that dimension formula would be applied before aggregation.
 * <p>
 * Example: {@literal @}DimensionFormula("IF({{reference}} &ge; 0, 'positive', 'negative')").
 *
 * Rules:
 * 1. The provided references should can be other dimension field defined in the same class, physical column in current
 *    physical table or a dot separated join path represent a logical dimension field defined in another table.
 * 2. Avoid cycle-reference.
 * 3. Sql expression can be carried when used with @JoinTo, the outer layer expression would be applied after the
 *    initial joined to sql expression.
 * <p>
 *
 * {@code expression} can also be composite. After {@link Dimension} construction, it will substitute attribute names
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
 *     {@literal @}DimensionFormula( "CASE WHEN {{overallRating}} = 'Good' THEN 1 ELSE 2 END")
 *     public int getPlayerLevel() {
 *         return playerLevel;
 *     }
 *
 *     {@literal @}JoinTo(path = "country.inUsa")
 *     public boolean isInUsa() {
 *         return inUsa;
 *     }
 *
 *     {@literal @}DimensionFormula( "CASE WHEN {{country.inUsa}} THEN 'true' ELSE 'false' END")
 *     public String getCountryIsInUsa() {
 *         return countryIsInUsa;
 *     }
 * }
 * }
 * </pre>
 * After {@link Dimension} construction, {@code countryIsInUsa} the provided expression will be substituted with
 * {@code country.inUsa}.
 */
@Documented
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface DimensionFormula {
    /**
     * The custom dimension expression that represents this dimension formula logic.
     *
     * @return dimension formula
     */
    String value();

    /**
     * The arguments accepted by this table.
     * @return arguments for the dimension
     */
    ArgumentDefinition[] arguments() default {};
}
