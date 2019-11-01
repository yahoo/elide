/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation;

import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.exceptions.InvalidOperationException;
import com.yahoo.elide.core.filter.FilterPredicate;
import com.yahoo.elide.core.filter.expression.AndFilterExpression;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.expression.NotFilterExpression;
import com.yahoo.elide.core.filter.expression.OrFilterExpression;
import com.yahoo.elide.datastores.aggregation.filter.visitor.FilterConstraints;
import com.yahoo.elide.datastores.aggregation.filter.visitor.SplitFilterExpressionVisitor;
import com.yahoo.elide.datastores.aggregation.metadata.models.AnalyticView;
import com.yahoo.elide.datastores.aggregation.metadata.models.Metric;
import com.yahoo.elide.datastores.aggregation.metadata.models.MetricFunction;
import com.yahoo.elide.datastores.aggregation.metadata.models.Table;
import com.yahoo.elide.datastores.aggregation.metadata.models.TimeDimension;
import com.yahoo.elide.datastores.aggregation.metadata.models.TimeDimensionGrain;
import com.yahoo.elide.datastores.aggregation.query.DimensionProjection;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.datastores.aggregation.query.TimeDimensionProjection;
import com.yahoo.elide.request.Argument;
import com.yahoo.elide.request.Attribute;
import com.yahoo.elide.request.EntityProjection;
import com.yahoo.elide.request.Relationship;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Helper for Aggregation Data Store which does the work associated with extracting {@link Query}.
 */
public class AggregationDataStoreHelper {
    private Table table;
    private EntityProjection entityProjection;
    private Set<DimensionProjection> dimensionProjections;
    private Set<TimeDimensionProjection> timeDimensions;
    private Map<Metric, MetricFunction> metricMap;
    private FilterExpression whereFilter;
    private FilterExpression havingFilter;

    public AggregationDataStoreHelper(Table table, EntityProjection entityProjection) {
        this.table = table;
        this.entityProjection = entityProjection;

        dimensionProjections = resolveNonTimeDimensions();
        timeDimensions = resolveTimeDimensions();

        if (table instanceof AnalyticView) {
            metricMap = resolveMetricMap(resolveMetricList());
            splitFilters();
        }
    }

    /**
     * Builds the query from internal state.
     * @return {@link Query} query object with all the parameters provided by user.
     */
    public Query getQuery() {
        return Query.builder()
                .table(table)
                .metrics(metricMap)
                .groupDimensions(dimensionProjections)
                .timeDimensions(timeDimensions)
                .whereFilter(whereFilter)
                .havingFilter(havingFilter)
                .sorting(entityProjection.getSorting())
                .pagination(entityProjection.getPagination())
                .build();
    }

    /**
     * Gets whereFilter and havingFilter based on provided filter expression from {@link EntityProjection}.
     */
    private void splitFilters() {
        FilterExpression filterExpression = entityProjection.getFilterExpression();
        if (filterExpression == null) {
            whereFilter = null;
            havingFilter = null;
            return;
        }
        SplitFilterExpressionVisitor visitor = new SplitFilterExpressionVisitor(table);
        FilterConstraints constraints = filterExpression.accept(visitor);
        whereFilter = constraints.getWhereExpression();
        havingFilter = constraints.getHavingExpression();

        if (havingFilter != null) {
            validateHavingClause(havingFilter);
        }
    }

    /**
     * Gets time dimensions based on relationships and attributes from {@link EntityProjection}.
     *
     * @return projections for time dimension columns
     * @throws InvalidOperationException Thrown if a requested time grain is not supported.
     */
    private Set<TimeDimensionProjection> resolveTimeDimensions() {
        return entityProjection.getAttributes().stream()
                .filter(attribute -> table.getTimeDimension(attribute.getName()) != null)
                .map(attribute -> {
                    TimeDimension timeDim = table.getTimeDimension(attribute.getName());

                    Argument grainArgument = attribute.getArguments().stream()
                            .filter(attr -> attr.getName().equals("grain"))
                            .findAny()
                            .orElse(null);

                    TimeDimensionGrain grain;
                    if (grainArgument == null) {
                        //The first grain is the default.
                        grain = timeDim.getSupportedGrains().stream()
                                .findFirst()
                                .orElseThrow(() -> new IllegalStateException(
                                        String.format("Requested default grain, no grain defined on %s",
                                                attribute.getName())));
                    } else {
                        String requestedGrainName = grainArgument.getValue().toString();

                        grain = timeDim.getSupportedGrains().stream()
                                .filter(g -> g.getGrain().name().equals(requestedGrainName))
                                .findFirst()
                                .orElseThrow(() -> new InvalidOperationException(
                                        String.format("Invalid grain %s", requestedGrainName)));
                    }

                    return DimensionProjection.toDimensionProjection(timeDim, grain.getGrain());
                })
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Gets dimensions except time dimensions based on relationships and attributes from {@link EntityProjection}.
     */
    private Set<DimensionProjection> resolveNonTimeDimensions() {
        Set<String> allColumns = getAttributes();
        allColumns.addAll(getRelationships());

        return allColumns.stream()
                .filter(columnName -> table.getTimeDimension(columnName) == null)
                .map(columnName -> table.getDimension(columnName))
                .filter(Objects::nonNull)
                .map(DimensionProjection::toDimensionProjection)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Gets metrics based on attributes from {@link EntityProjection}.
     */
    private Set<Metric> resolveMetricList() {
        Set<String> attributeNames = getAttributes();

        return table.getColumns(Metric.class).stream()
                .filter(m -> attributeNames.contains(m.getName()))
                .collect(Collectors.toSet());
    }

    /**
     * Constructs map between {@link Metric} objects and the type of {@link MetricFunction} we want to use.
     * @param metrics Set of {@link Metric} objects.
     */
    private Map<Metric, MetricFunction> resolveMetricMap(Set<Metric> metrics) {
        if (metrics.isEmpty()) {
            throw new InvalidOperationException("Must provide at least one metric in query");
        }

        return metrics.stream().collect(
                Collectors.toMap(
                        Function.identity(),
                        Metric::getMetricFunction));
    }

    /**
     * Gets attribute names from {@link EntityProjection}.
     * @return attributes list of {@link Attribute} names
     */
    private Set<String> getAttributes() {
        return entityProjection.getAttributes().stream()
                .map(Attribute::getName).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Gets relationship names from {@link EntityProjection}.
     * @return relationships list of {@link Relationship} names
     */
    private Set<String> getRelationships() {
        return entityProjection.getRelationships().stream()
                .map(Relationship::getName).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Validate the having clause before execution. Having clause is not as flexible as where clause,
     * the fields in having clause must be either or these two:
     * 1. A grouped by dimension in this query
     * 2. An aggregated metric in this query
     *
     * All grouped by dimensions are defined in the entity bean, so the last entity class of a filter path
     * must match entity class of the query.
     *
     * @param havingClause having clause generated from this query
     */
    private void validateHavingClause(FilterExpression havingClause) {
        if (havingClause instanceof FilterPredicate) {
            Path path = ((FilterPredicate) havingClause).getPath();
            Path.PathElement last = path.lastElement().get();
            Class<?> cls = last.getType();
            String fieldName = last.getFieldName();

            if (cls != table.getCls()) {
                throw new InvalidOperationException(
                        String.format(
                                "Classes don't match when try filtering on %s in having clause of %s.",
                                cls.getSimpleName(),
                                table.getCls().getSimpleName()));
            }

            if (table.isMetric(fieldName)) {
                Metric metric = table.getMetric(fieldName);
                if (!metricMap.containsKey(metric)) {
                    throw new InvalidOperationException(
                            String.format(
                                    "Metric field %s must be aggregated before filtering in having clause.",
                                    fieldName));
                }
            } else {
                if (dimensionProjections.stream().noneMatch(dim -> dim.getName().equals(fieldName))) {
                    throw new InvalidOperationException(
                            String.format(
                                    "Dimension field %s must be grouped before filtering in having clause.",
                                    fieldName));
                }
            }
        } else if (havingClause instanceof AndFilterExpression) {
            validateHavingClause(((AndFilterExpression) havingClause).getLeft());
            validateHavingClause(((AndFilterExpression) havingClause).getRight());
        } else if (havingClause instanceof OrFilterExpression) {
            validateHavingClause(((OrFilterExpression) havingClause).getLeft());
            validateHavingClause(((OrFilterExpression) havingClause).getRight());
        } else if (havingClause instanceof NotFilterExpression) {
            validateHavingClause(((NotFilterExpression) havingClause).getNegated());
        }
    }
}
