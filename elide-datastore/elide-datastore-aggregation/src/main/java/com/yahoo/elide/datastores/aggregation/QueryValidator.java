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
import com.yahoo.elide.datastores.aggregation.metadata.metric.MetricFunctionInvocation;
import com.yahoo.elide.datastores.aggregation.metadata.models.Table;
import com.yahoo.elide.datastores.aggregation.query.ColumnProjection;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.request.Sorting;

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
    private Table queriedTable;
    private List<MetricFunctionInvocation> metrics;
    private Set<ColumnProjection> dimensionProjections;

    public QueryValidator(Query query, Set<String> allFields, EntityDictionary dictionary) {
        this.query = query;
        this.allFields = allFields;
        this.dictionary = dictionary;
        this.queriedTable = query.getTable();
        this.metrics = query.getMetrics();
        this.dimensionProjections = query.getDimensions();
    }

    /**
     * Method that handles all checks to make sure query is valid before we attempt to execute the query.
     */
    public void validate() {
        validateHavingClause(query.getHavingFilter());
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
            Class<?> cls = last.getType();
            String fieldName = last.getFieldName();

            Class<?> tableClass = dictionary.getEntityClass(queriedTable.getId());

            if (cls != tableClass) {
                throw new InvalidOperationException(
                        String.format(
                                "Can not filter on relationship field %s in HAVING clause when querying table %s.",
                                path.toString(),
                                tableClass.getSimpleName()));
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

        Path.PathElement currentElement = pathElements.get(0);
        String currentField = currentElement.getFieldName();
        Class<?> currentClass = currentElement.getType();

        // TODO: support sorting using alias
        if (allFields.stream().noneMatch(field -> field.equals(currentField))) {
            throw new InvalidOperationException("Can not sort on " + currentField + " as it is not present in query");
        }
        if (dictionary.getIdFieldName(currentClass).equals(currentField)
                || currentField.equals(EntityDictionary.REGULAR_ID_NAME)) {
            throw new InvalidOperationException("Sorting on id field is not permitted");
        }
    }
}
