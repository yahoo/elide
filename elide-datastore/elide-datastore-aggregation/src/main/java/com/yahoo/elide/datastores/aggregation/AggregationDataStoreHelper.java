/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.exceptions.InvalidOperationException;
import com.yahoo.elide.core.filter.FilterPredicate;
import com.yahoo.elide.core.filter.expression.AndFilterExpression;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.expression.NotFilterExpression;
import com.yahoo.elide.core.filter.expression.OrFilterExpression;
import com.yahoo.elide.core.sort.Sorting;
import com.yahoo.elide.datastores.aggregation.filter.visitor.FilterConstraints;
import com.yahoo.elide.datastores.aggregation.filter.visitor.SplitFilterExpressionVisitor;
import com.yahoo.elide.datastores.aggregation.metadata.metric.MetricFunctionInvocation;
import com.yahoo.elide.datastores.aggregation.metadata.models.AnalyticView;
import com.yahoo.elide.datastores.aggregation.metadata.models.Dimension;
import com.yahoo.elide.datastores.aggregation.metadata.models.Table;
import com.yahoo.elide.datastores.aggregation.metadata.models.TimeDimension;
import com.yahoo.elide.datastores.aggregation.metadata.models.TimeDimensionGrain;
import com.yahoo.elide.datastores.aggregation.query.ColumnProjection;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.datastores.aggregation.query.TimeDimensionProjection;
import com.yahoo.elide.request.Argument;
import com.yahoo.elide.request.Attribute;
import com.yahoo.elide.request.EntityProjection;
import com.yahoo.elide.request.Relationship;

import com.google.common.collect.Sets;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Helper for Aggregation Data Store which does the work associated with extracting {@link Query}.
 */
public class AggregationDataStoreHelper {

    private AnalyticView queriedTable;

    //TODO refactor this class in the next PR
    //TODO Add support for user selected metrics.
    private static final int AGGREGATION_METHOD_INDEX = 0;

    private EntityProjection entityProjection;
    private Set<ColumnProjection> dimensionProjections;
    private Set<TimeDimensionProjection> timeDimensions;
    private List<MetricFunctionInvocation> metrics;
    private FilterExpression whereFilter;
    private FilterExpression havingFilter;
    private EntityDictionary dictionary;

    public AggregationDataStoreHelper(Table table, EntityProjection entityProjection, EntityDictionary dictionary) {
        if (!(table instanceof AnalyticView)) {
            throw new InvalidOperationException("Queried table is not analyticView: " + table.getName());
        }

        this.queriedTable = (AnalyticView) table;
        this.entityProjection = entityProjection;
        this.dictionary = dictionary;
        dimensionProjections = resolveNonTimeDimensions();
        timeDimensions = resolveTimeDimensions();
        metrics = resolveMetrics();
        validateSorting();
        splitFilters();
    }

    /**
     * Builds the query from internal state.
     * @return {@link Query} query object with all the parameters provided by user.
     */
    public Query getQuery() {
        return Query.builder()
                .analyticView(queriedTable)
                .metrics(metrics)
                .groupByDimensions(dimensionProjections)
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
        SplitFilterExpressionVisitor visitor = new SplitFilterExpressionVisitor(queriedTable);
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
                .filter(attribute -> queriedTable.getTimeDimension(attribute.getName()) != null)
                .map(timeDimAttr -> {
                    TimeDimension timeDim = queriedTable.getTimeDimension(timeDimAttr.getName());

                    Argument grainArgument = timeDimAttr.getArguments().stream()
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
                                                timeDimAttr.getName())));
                    } else {
                        String requestedGrainName = grainArgument.getValue().toString();

                        grain = timeDim.getSupportedGrains().stream()
                                .filter(g ->
                                        g.getGrain().name().toLowerCase(Locale.ENGLISH).equals(requestedGrainName))
                                .findFirst()
                                .orElseThrow(() -> new InvalidOperationException(
                                        String.format("Invalid grain %s", requestedGrainName)));
                    }

                    return ColumnProjection.toProjection(timeDim, grain.getGrain(), timeDimAttr.getAlias());
                })
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Gets dimensions except time dimensions based on relationships and attributes from {@link EntityProjection}.
     */
    private Set<ColumnProjection> resolveNonTimeDimensions() {
        Set<ColumnProjection> attributes = entityProjection.getAttributes().stream()
                .filter(attribute -> queriedTable.getTimeDimension(attribute.getName()) == null)
                .map(dimAttr -> {
                    Dimension dimension = queriedTable.getDimension(dimAttr.getName());
                    return dimension == null ? null : ColumnProjection.toProjection(dimension, dimAttr.getAlias());
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<ColumnProjection> relationships = entityProjection.getRelationships().stream()
                .map(dimAttr -> {
                    Dimension dimension = queriedTable.getDimension(dimAttr.getName());
                    return dimension == null ? null : ColumnProjection.toProjection(dimension, dimAttr.getAlias());
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        return Sets.union(attributes, relationships);
    }

    /**
     * Gets metrics based on attributes from {@link EntityProjection}.
     */
    private List<MetricFunctionInvocation> resolveMetrics() {
        return entityProjection.getAttributes().stream()
                .filter(attribute -> queriedTable.isMetric(attribute.getName()))
                .map(attribute -> queriedTable.getMetric(attribute.getName())
                        .getMetricFunction()
                        .invoke(attribute.getArguments(), attribute.getAlias()))
                .collect(Collectors.toList());
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
     * Gets attribute names from {@link EntityProjection}.
     * @return relationships list of {@link Attribute} names
     */
    private Set<String> getAttributes() {
        return entityProjection.getAttributes().stream()
                .map(Attribute::getName).collect(Collectors.toCollection(LinkedHashSet::new));
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
        // TODO: support having clause for alias
        if (havingClause instanceof FilterPredicate) {
            Path path = ((FilterPredicate) havingClause).getPath();
            Path.PathElement last = path.lastElement().get();
            Class<?> cls = last.getType();
            String fieldName = last.getFieldName();

            if (cls != queriedTable.getCls()) {
                throw new InvalidOperationException(
                        String.format(
                                "Classes don't match when try filtering on %s in having clause of %s.",
                                cls.getSimpleName(),
                                queriedTable.getCls().getSimpleName()));
            }

            if (queriedTable.isMetric(fieldName)) {
                if (metrics.stream().noneMatch(m -> m.getAlias().equals(fieldName))) {
                    throw new InvalidOperationException(
                            String.format(
                                    "Metric field %s must be aggregated before filtering in having clause.",
                                    fieldName));
                }
            } else {
                if (dimensionProjections.stream().noneMatch(dim -> dim.getAlias().equals(fieldName))) {
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

    /**
     * Method to verify that all the sorting options provided
     * by the user are valid and supported.
     */
    public void validateSorting() {
        Sorting sorting = entityProjection.getSorting();
        if (sorting == null) {
            return;
        }
        Set<String> allFields = getRelationships();
        allFields.addAll(getAttributes());
        Map<Path, Sorting.SortOrder> sortClauses = sorting.getValidSortingRules(entityProjection.getType(), dictionary);
        sortClauses.keySet().forEach((path) -> validateSortingPath(path, allFields));
    }

    /**
     * Verifies that the current path can be sorted on
     * @param path The path that we are validating
     * @param allFields Set of all field names included in initial query
     */
    private void validateSortingPath(Path path, Set<String> allFields) {
        List<Path.PathElement> pathElemenets = path.getPathElements();

        //TODO add support for double nested sorting
        if (pathElemenets.size() > 2) {
            throw new UnsupportedOperationException(
                    "Currently sorting on double nested fields is not supported");
        }
        Path.PathElement currentElement = pathElemenets.get(0);
        String currentField = currentElement.getFieldName();
        Class<?> currentClass = currentElement.getType();
        if (!allFields.stream().anyMatch(field -> field.equals(currentField))) {
            throw new InvalidOperationException("Can't sort on " + currentField + " as it is not present in query");
        }
        if (dictionary.getIdFieldName(currentClass).equals(currentField)
                || currentField.equals(EntityDictionary.REGULAR_ID_NAME)) {
            throw new InvalidOperationException("Sorting on id field is not permitted");
        }
    }
}
