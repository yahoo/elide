/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation;

import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.exceptions.InvalidOperationException;
import com.yahoo.elide.core.filter.expression.AndFilterExpression;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.expression.NotFilterExpression;
import com.yahoo.elide.core.filter.expression.OrFilterExpression;
import com.yahoo.elide.core.filter.predicates.FilterPredicate;
import com.yahoo.elide.core.request.Sorting;
import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.datastores.aggregation.query.ColumnProjection;
import com.yahoo.elide.datastores.aggregation.query.MetricProjection;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.datastores.aggregation.query.Queryable;
import com.yahoo.elide.datastores.aggregation.query.TimeDimensionProjection;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class that checks whether a constructed {@link Query} object can be executed.
 * Checks include validate sorting, having clause and make sure there is at least 1 metric queried.
 */
public class QueryValidator {
    private Query query;
    private Set<String> allFields;
    private EntityDictionary dictionary;
    private Queryable queriedTable;
    private Set<MetricProjection> metrics;
    private Set<ColumnProjection> dimensionProjections;

    public QueryValidator(Query query, Set<String> allFields, EntityDictionary dictionary) {
        this.query = query;
        this.allFields = allFields;
        this.dictionary = dictionary;
        this.queriedTable = query.getSource();
        this.metrics = query.getMetricProjections();
        this.dimensionProjections = query.getAllDimensionProjections();
    }

    /**
     * Method that handles all checks to make sure query is valid before we attempt to execute the query.
     */
    public void validate() {
        validateWhereClause(query.getWhereFilter());
        validateHavingClause(query.getHavingFilter());
        validateTimeDimensions(query.getTimeDimensionProjections());
        validateSorting();
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
            String fieldName = last.getFieldName();

            if (path.getPathElements().size() > 1) {
                throw new InvalidOperationException("Relationship traversal not supported for analytic queries.");
            }

            if (queriedTable.getMetricProjection(fieldName) != null) {
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
     * Ensures that no filter predicates tries not navigate a relationship.
     * @param whereClause
     */
    private void validateWhereClause(FilterExpression whereClause) {
        // TODO: support having clause for alias
        if (whereClause instanceof FilterPredicate) {
            Path path = ((FilterPredicate) whereClause).getPath();
            if (path.getPathElements().size() > 1) {
                throw new InvalidOperationException("Relationship traversal not supported for analytic queries.");
            }
        } else if (whereClause instanceof AndFilterExpression) {
            validateWhereClause(((AndFilterExpression) whereClause).getLeft());
            validateWhereClause(((AndFilterExpression) whereClause).getRight());
        } else if (whereClause instanceof OrFilterExpression) {
            validateWhereClause(((OrFilterExpression) whereClause).getLeft());
            validateWhereClause(((OrFilterExpression) whereClause).getRight());
        } else if (whereClause instanceof NotFilterExpression) {
            validateWhereClause(((NotFilterExpression) whereClause).getNegated());
        }
    }

    /**
     * Method to verify that all the sorting options provided
     * by the user are valid and supported.
     */
    public void validateSorting() {
        Sorting sorting = query.getSorting();
        if (sorting == null) {
            return;
        }
        Map<Path, Sorting.SortOrder> sortClauses = sorting.getSortingPaths();
        sortClauses.keySet().forEach((path) -> validateSortingPath(path, allFields));
    }

    /**
     * Verifies that the current path can be sorted on
     * @param path The path that we are validating
     * @param allFields Set of all field names included in initial query
     */
    private void validateSortingPath(Path path, Set<String> allFields) {
        List<Path.PathElement> pathElements = path.getPathElements();

        if (pathElements.size() > 1) {
            throw new InvalidOperationException("Relationship traversal not supported for analytic queries.");
        }

        Path.PathElement currentElement = pathElements.get(0);
        String currentField = currentElement.getFieldName();
        Type<?> currentClass = currentElement.getType();

        // TODO: support sorting using alias
        if (allFields.stream().noneMatch(currentField::equals)) {
            throw new InvalidOperationException("Can not sort on " + currentField + " as it is not present in query");
        }
        if (dictionary.getIdFieldName(currentClass).equals(currentField)
                || currentField.equals(EntityDictionary.REGULAR_ID_NAME)) {
            throw new InvalidOperationException("Sorting on id field is not permitted");
        }
    }

    private void validateTimeDimensions(Set<TimeDimensionProjection> projections) {
        projections.stream().forEach(clientDimension -> {
            queriedTable.getTimeDimensionProjections().stream()
                    .map(TimeDimensionProjection::getGrain)
                    .filter((tableDimensionGrain) -> tableDimensionGrain.equals(clientDimension.getGrain()))
                    .findAny()
                    .orElseThrow(() ->
                            new InvalidOperationException("Invalid time grain: " + clientDimension.getGrain()));
        });
    }
}
