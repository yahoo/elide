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
import com.yahoo.elide.datastores.aggregation.query.DimensionProjection;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.datastores.aggregation.schema.Schema;
import com.yahoo.elide.datastores.aggregation.schema.metric.Aggregation;
import com.yahoo.elide.datastores.aggregation.schema.metric.Metric;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class QueryValidator {

    private Set<String> allFields;
    private Schema schema;
    private Query query;
    private EntityDictionary dictionary;
    private Set<DimensionProjection> dimensionProjections;
    private Map<Metric, Class<? extends Aggregation>> metricMap;
    private Class<?> type;

    public QueryValidator(Set<String> allFields, Query query, EntityDictionary dictionary, Class<?> type) {
        this.allFields = allFields;
        this.schema = query.getSchema();
        this.query = query;
        this.dictionary = dictionary;
        this.type = type;
        dimensionProjections = query.getDimensions();
        metricMap = query.getMetrics();
    }

    /**
     * Method that handles all checks to make sure query is valid before we attempt to execute the query.
     */
    public void validate() {
        FilterExpression havingClause = query.getHavingFilter();
        validateHavingClause(havingClause);
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
        if (havingClause instanceof FilterPredicate) {
            Path path = ((FilterPredicate) havingClause).getPath();
            Path.PathElement last = path.lastElement().get();
            Class<?> cls = last.getType();
            String field = last.getFieldName();

            if (cls != schema.getEntityClass()) {
                throw new InvalidOperationException(
                        String.format(
                                "Classes don't match when try filtering on %s in having clause of %s.",
                                cls.getSimpleName(),
                                schema.getEntityClass().getSimpleName()));
            }

            if (schema.isMetricField(field)) {
                Metric metric = schema.getMetric(field);
                if (!metricMap.containsKey(metric)) {
                    throw new InvalidOperationException(
                            String.format(
                                    "Metric field %s must be aggregated before filtering in having clause.", field));
                }
            } else {
                if (dimensionProjections.stream().noneMatch(dim -> dim.getName().equals(field))) {
                    throw new InvalidOperationException(
                            String.format(
                                    "Dimension field %s must be grouped before filtering in having clause.", field));
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

        Map<Path, Sorting.SortOrder> sortClauses = sorting.getValidSortingRules(type, dictionary);
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
