/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.datastores.aggregation.query;

import com.paiondata.elide.core.filter.expression.FilterExpression;
import com.paiondata.elide.core.filter.expression.PredicateExtractionVisitor;
import com.paiondata.elide.core.filter.predicates.FilterPredicate;
import com.paiondata.elide.core.request.Argument;
import com.paiondata.elide.datastores.aggregation.queryengines.sql.ConnectionDetails;
import com.paiondata.elide.datastores.aggregation.queryengines.sql.dialects.SQLDialect;
import com.paiondata.elide.datastores.aggregation.queryengines.sql.metadata.SQLJoin;
import com.google.common.collect.Streams;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Represents something that can be queried.  There are generally, three kinds:
 * - Tables
 * - Queries (a subquery)
 * - Joins (a join between multiple tables or subqueries).
 */
public interface Queryable {

    /**
     * Returns the source of this queryable's data.  The source could be itself.
     * @return The data source.
     */
    Queryable getSource();

    /**
     * Every queryable needs an alias which uniquely identifies the queryable in an individual query.
     * @return The alias
     */
    default String getAlias() {
        //Eliminate any negative hash codes.
        return getSource().getAlias() + "_" + (hashCode() & 0x7fffffff);
    }

    /**
     * The name of the queryable.
     * @return The name
     */
    default String getName() {
        return getAlias();
    }

    /**
     * The version of the queryable.
     * @return The version
     */
    default String getVersion() {
        return "";
    }

    /**
     * Retrieves a column by name.
     * @param name The alias of the column.
     * @return The column.
     */
    default ColumnProjection getColumnProjection(String name) {
        return getColumnProjections().stream()
                .filter(dim -> dim.getAlias().equals(name))
                .findFirst()
                .orElse(null);
    }

    /**
     * Retrieves a column by name and arguments.
     * @param name The alias of the column.
     * @param arguments Arguments provided for the column.
     * @return The column.
     */
    default ColumnProjection getColumnProjection(String name, Map<String, Argument> arguments, boolean isProjected) {
        return getColumnProjections().stream()
                .filter(dim -> dim.isProjected() == isProjected)
                .filter(dim -> dim.getAlias().equals(name) && dim.getArguments().equals(arguments))
                .findFirst()
                .orElse(null);
    }

    /**
     * Retrieves a column by name and arguments.
     * @param name The alias of the column.
     * @param arguments Arguments provided for the column.
     * @return The column.
     */
    default ColumnProjection getColumnProjection(String name, Map<String, Argument> arguments) {
        return getColumnProjections().stream()
                .filter(dim -> dim.getAlias().equals(name) && dim.getArguments().equals(arguments))
                .findFirst()
                .orElse(null);
    }

    /**
     * Retrieves a column by filter predicate.
     * @param filterPredicate A predicate to search for column projections.
     * @return The column.
     */
    default ColumnProjection getColumnProjection(Predicate<ColumnProjection> filterPredicate) {
        return getColumnProjections().stream()
                .filter(filterPredicate)
                .findFirst()
                .orElse(null);
    }

    /**
     * Retrieves a non-time dimension by name.
     * @param name The name of the dimension.
     * @return The dimension.
     */
    default DimensionProjection getDimensionProjection(String name) {
        return getDimensionProjections().stream()
                .filter(dim -> dim.getAlias().equals(name))
                .findFirst()
                .orElse(null);
    }

    /**
     * Retrieves all the non-time dimensions.
     * @return The non-time dimensions.
     */
    List<DimensionProjection> getDimensionProjections();

    /**
     * Retrieves a metric by name.
     * @param name The name of the metric.
     * @return The metric.
     */
    default MetricProjection getMetricProjection(String name) {
        return getMetricProjections().stream()
                .filter(metric -> metric.getAlias().equals(name))
                .findFirst()
                .orElse(null);
    }

    /**
     * Retrieves all the metrics.
     * @return The metrics.
     */
    List<MetricProjection> getMetricProjections();

    /**
     * Retrieves a time dimension by name.
     * @param name The name of the time dimension.
     * @return The time dimension.
     */
    default TimeDimensionProjection getTimeDimensionProjection(String name) {
        return getTimeDimensionProjections().stream()
                .filter(dim -> dim.getAlias().equals(name))
                .findFirst()
                .orElse(null);
    }

    /**
     * Retrieves all the time dimensions.
     * @return The time dimensions.
     */
    List<TimeDimensionProjection> getTimeDimensionProjections();

    /**
     * Returns all the columns.
     * @return the columns.
     */
    default List<ColumnProjection> getColumnProjections() {
        return Streams.concat(
                getTimeDimensionProjections().stream(),
                getDimensionProjections().stream(),
                getMetricProjections().stream())
                .collect(Collectors.toList());
    }


    default <T extends ColumnProjection> List<T> getFilterProjections(
            FilterExpression expression,
            Class<T> columnType
    ) {
        return getFilterProjections(expression).stream()
                .filter(columnType::isInstance)
                .map(columnType::cast)
                .collect(Collectors.toList());
    }

    default FilterExpression getWhereFilter() {
        return null;
    }

    default List<ColumnProjection> getFilterProjections(FilterExpression expression) {
        List<ColumnProjection> results = new ArrayList<>();
        if (expression == null) {
            return results;
        }

        Collection<FilterPredicate> predicates = expression.accept(new PredicateExtractionVisitor());

        predicates.stream().forEach((predicate -> {
            Map<String, Argument> arguments = new HashMap<>();

            predicate.getPath().lastElement().get().getArguments().forEach(argument ->
                    arguments.put(argument.getName(), argument)
            );

            ColumnProjection projection = getSource().getColumnProjection(predicate.getField(), arguments);

            results.add(projection);
        }));

        return results;
    }

    /**
     * Returns the connection details where this queryable is sourced from.
     * @return the connectinon details
     */
    default ConnectionDetails getConnectionDetails() {
        return getRoot().getConnectionDetails();
    }

    /**
     * Execute a visitor on this query.
     * @param visitor The visitor to execute.
     * @param <T> The return type of the visitor.
     * @return Something that the visitor is constructing.
     */
    default <T> T accept(QueryVisitor<T> visitor) {
        return visitor.visitQueryable(this);
    }

    /**
     * Determines if this queryable is nested from another queryable.
     * @return true if the source is another queryable.  False otherwise.
     */
    default boolean isNested() {
        Queryable source = getSource();

        //A table with no source is not nested.  Neither is a query with a source table.
        return (source != null && source.getSource() != source);
    }

    /**
     * Gets the root table for the queryable.
     * @return the root table.
     */
    default Queryable getRoot() {
        Queryable current = this;
        while (current.isNested()) {
            current = current.getSource();
        }

        return current.getSource();
    }

    /**
     * Determines if this queryable is root table.
     * @return true if this queryable is root table.
     */
    default boolean isRoot() {
        return this == this.getRoot();
    }

    /**
     * Returns the depth of the nesting of this Queryable.
     * @return 0 for unnested.  Positive integer for nested..
     */
    default int nestDepth() {
        int depth = 0;
        Queryable current = this;
        while (current.isNested()) {
            depth++;
            current = current.getSource();
        }
        return depth;
    }

    /**
     * Get the joins associated with table source of this queryable.
     * @return A map of join name and {@link SQLJoin}.
     */
    default Map<String, SQLJoin> getJoins() {
        return this.getRoot().getJoins();
    }

    /**
     * Get the join info for the given join column.
     * @param joinName Join Name.
     * @return {@link SQLJoin} for the given join column.
     */
    default SQLJoin getJoin(String joinName) {
        return getJoins().get(joinName);
    }

    /**
     * Check if table source of this queryable has the given join column.
     * @param joinName Join Name.
     * @return true if table source of this queryable has the given join column.
     */
    default boolean hasJoin(String joinName) {
        return getJoins().containsKey(joinName);
    }

    /**
     * Get the dialect info for the table source of this queryable.
     * @return {@link SQLDialect} for the table source.
     */
    default SQLDialect getDialect() {
        return getConnectionDetails().getDialect();
    }

    /**
     * Converts a filter expression into a set of ColumnProjections.
     * @param query The parent query.
     * @param expression The filter expression to extract.
     * @return A set of zero or more column projections with their arguments.
     */
    static Set<ColumnProjection> extractFilterProjections(Queryable query, FilterExpression expression) {
        if (expression == null) {
            return new LinkedHashSet<>();
        }

        Collection<FilterPredicate> predicates = expression.accept(new PredicateExtractionVisitor());

        Set<ColumnProjection> filterProjections = new LinkedHashSet<>();
        predicates.stream().forEach((predicate -> {
            Map<String, Argument> arguments = new HashMap<>();

            predicate.getPath().lastElement().get().getArguments().forEach(argument ->
                    arguments.put(argument.getName(), argument)
            );

            ColumnProjection projection = query.getSource().getColumnProjection(predicate.getField(), arguments);

            if (projection != null) {
                filterProjections.add(projection);
            }
        }));

        return filterProjections;
    }

    /**
     * Gets the available arguments for this queryable.
     * @return available arguments for this queryable as map of String and {@link Argument}.
     */
    default Map<String, Argument> getArguments() {
        return Collections.emptyMap();
    }
}
