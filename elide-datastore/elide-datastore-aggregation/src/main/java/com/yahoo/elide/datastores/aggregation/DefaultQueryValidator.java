/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation;

import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.exceptions.InvalidOperationException;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.expression.PredicateExtractionVisitor;
import com.yahoo.elide.core.request.Sorting;
import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.datastores.aggregation.metadata.enums.ValueType;
import com.yahoo.elide.datastores.aggregation.metadata.models.Table;
import com.yahoo.elide.datastores.aggregation.query.ColumnProjection;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.datastores.aggregation.query.Queryable;
import com.yahoo.elide.datastores.aggregation.query.TimeDimensionProjection;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLTable;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Class that checks whether a constructed {@link Query} object can be executed.
 * Checks include validate sorting, having clause and make sure there is at least 1 metric queried.
 */
public class DefaultQueryValidator implements QueryValidator {
    private EntityDictionary dictionary;

    public DefaultQueryValidator(EntityDictionary dictionary) {
        this.dictionary = dictionary;
    }

    @Override
    public void validateHavingClause(Query query) {

        FilterExpression havingClause = query.getHavingFilter();
        Queryable source = query.getSource();

        if (havingClause == null) {
            return;
        }

        havingClause.accept(new PredicateExtractionVisitor()).forEach(predicate -> {
            Path path = predicate.getPath();
            Path.PathElement last = path.lastElement().get();
            String fieldName = last.getFieldName();

            if (path.getPathElements().size() > 1) {
                throw new InvalidOperationException("Relationship traversal not supported for analytic queries.");
            }

            if (source.getDimensionProjection(fieldName) != null) {
                if (query.getAllDimensionProjections().stream().noneMatch(dim -> dim.getAlias().equals(fieldName))) {
                    throw new InvalidOperationException(
                            String.format(
                                    "Dimension field %s must be grouped before filtering in having clause.",
                                    fieldName));
                }
            }

            if (source.getTimeDimensionProjection(fieldName) != null) {
                query.getAllDimensionProjections()
                        .stream()
                        .filter(dim -> dim.getAlias().equals(fieldName)
                                && TimeDimensionProjection.class.isAssignableFrom(dim.getClass()))
                        .forEach(dim -> {
                            Object grain = dim.getArguments().get("grain").getValue();
                            if (last.getArguments().stream().noneMatch(arg -> (arg.getValue()).equals(grain))) {
                                throw new InvalidOperationException(
                                        String.format(
                                                "Time Dimension field %s must use the same grain argument "
                                                        + "in the projection and the having clause.", fieldName));
                            }
                        });
            }
        });
    }


    @Override
    public void validateWhereClause(Query query) {

        FilterExpression whereClause = query.getWhereFilter();

        if (whereClause == null) {
            return;
        }

        whereClause.accept(new PredicateExtractionVisitor()).forEach(predicate -> {
            Path path = predicate.getPath();
            if (path.getPathElements().size() > 1) {
                throw new InvalidOperationException("Relationship traversal not supported for analytic queries.");
            }
        });
    }

    @Override
    public void validateSorting(Query query) {
        Sorting sorting = query.getSorting();

        if (sorting == null) {
            return;
        }

        Map<Path, Sorting.SortOrder> sortClauses = sorting.getSortingPaths();
        Set<String> allFields = query.getColumnProjections()
                .stream()
                .map(ColumnProjection::getAlias)
                .collect(Collectors.toSet());

        sortClauses.keySet().forEach((path) -> validateSortingPath(path, allFields));
    }

    /**
     * Verifies that the current path can be sorted on.
     * @param path The path that we are validating
     * @param allFields Set of all field names included in initial query
     */
    private void validateSortingPath(Path path, Set<String> allFields) {
        List<Path.PathElement> pathElements = path.getPathElements();

        if (pathElements.size() > 1) {
            throw new InvalidOperationException("Relationship traversal not supported for analytic queries.");
        }

        Path.PathElement currentElement = pathElements.get(0);
        String currentField = currentElement.getAlias();
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


    @Override
    public void validateProjectedColumns(Query query) {
        boolean onlyId = query.getColumnProjections().stream()
                .allMatch(column -> column.getValueType().equals(ValueType.ID));

        if (((SQLTable) query.getSource()).isFact() && onlyId) {
            throw new InvalidOperationException("Cannot query a fact table only by ID");
        }
    }
}
