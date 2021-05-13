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
import com.yahoo.elide.core.request.Argument;
import com.yahoo.elide.core.request.Sorting;
import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.datastores.aggregation.metadata.enums.ValueType;
import com.yahoo.elide.datastores.aggregation.metadata.models.Column;
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
    protected EntityDictionary dictionary;

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
    protected void validateSortingPath(Path path, Set<String> allFields) {
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

        //TODO - We should generate ID metric columns as ROW_NUMBER() if they do not exist.
        boolean onlyId = query.getColumnProjections().stream()
                .allMatch(column -> column.getValueType().equals(ValueType.ID));

        if (onlyId) {
            throw new InvalidOperationException("Cannot query a table only by ID");
        }

        query.getColumnProjections().forEach(column -> validateColumnArguments(query, column));
    }

    @Override
    public void validateQueryArguments(Query query) {
        SQLTable table = (SQLTable) query.getSource();

        table.getArguments().forEach(tableArgument -> {
            if (query.getArguments().containsKey(tableArgument.getName())) {
                Argument clientArgument = query.getArguments().get(tableArgument.getName());

                validateArgument(clientArgument, tableArgument);
            }
        });
    }

    protected void validateColumnArguments(Query query, ColumnProjection projection) {
        SQLTable table = (SQLTable) query.getSource();
        Column column = table.getColumn(Column.class, projection.getName());

        column.getArguments().forEach(columnArgument -> {
            if (projection.getArguments().containsKey(columnArgument.getName())) {
                Argument clientArgument = projection.getArguments().get(columnArgument.getName());

                validateArgument(clientArgument, columnArgument);
            }
        });
    }

    protected void validateArgument(
            Argument clientArgument,
            com.yahoo.elide.datastores.aggregation.metadata.models.Argument argumentDefinition
    ) {

        boolean isValid = argumentDefinition.getType().matches(clientArgument.getValue().toString());

        if (!isValid) {
            throw new InvalidOperationException(String.format("Argument %s has an invalid value: %s",
                    argumentDefinition.getName(),
                    clientArgument.getValue()));
        }
    }
}
