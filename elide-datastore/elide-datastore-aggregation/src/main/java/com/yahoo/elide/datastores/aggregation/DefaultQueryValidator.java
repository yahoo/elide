/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation;

import static com.yahoo.elide.datastores.aggregation.query.Queryable.extractFilterProjections;

import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.exceptions.InvalidOperationException;
import com.yahoo.elide.core.filter.Operator;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.expression.PredicateExtractionVisitor;
import com.yahoo.elide.core.filter.predicates.FilterPredicate;
import com.yahoo.elide.core.request.Argument;
import com.yahoo.elide.core.request.Sorting;
import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.datastores.aggregation.metadata.enums.ValueType;
import com.yahoo.elide.datastores.aggregation.metadata.models.ArgumentDefinition;
import com.yahoo.elide.datastores.aggregation.metadata.models.Column;
import com.yahoo.elide.datastores.aggregation.query.ColumnProjection;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLTable;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Class that checks whether a constructed {@link Query} object can be executed.
 * Checks include validate sorting, having clause and make sure there is at least 1 metric queried.
 */
public class DefaultQueryValidator implements QueryValidator {
    private static final Set<Operator> REGEX_OPERATORS = Stream.of(Operator.INFIX, Operator.INFIX_CASE_INSENSITIVE,
            Operator.POSTFIX, Operator.POSTFIX_CASE_INSENSITIVE, Operator.PREFIX, Operator.PREFIX_CASE_INSENSITIVE)
        .collect(Collectors.toCollection(HashSet::new));

    protected EntityDictionary dictionary;

    public DefaultQueryValidator(EntityDictionary dictionary) {
        this.dictionary = dictionary;
    }

    @Override
    public void validateHavingClause(Query query) {
        FilterExpression havingClause = query.getHavingFilter();

        if (havingClause == null) {
            return;
        }

        havingClause.accept(new PredicateExtractionVisitor()).forEach(predicate -> {
            Path path = predicate.getPath();

            if (path.getPathElements().size() > 1) {
                throw new InvalidOperationException("Relationship traversal not supported for analytic queries.");
            }

            validatePredicate(query, predicate);

            extractFilterProjections(query, havingClause).stream().forEach(projection -> {

                Predicate<ColumnProjection> filterByNameAndArgs =
                        (column) -> (column.getAlias().equals(projection.getAlias())
                                || column.getName().equals(projection.getName()))
                                && column.getArguments().equals(projection.getArguments());

                //Query by (alias or name) and arguments.   The filter may or may not be using the alias.
                if (query.getColumnProjection(filterByNameAndArgs) == null) {

                    Predicate<ColumnProjection> filterByName =
                            (column) -> (column.getAlias().equals(projection.getAlias())
                                    || column.getName().equals(projection.getName()));

                    //The column wasn't projected at all.
                    if (query.getColumnProjection(filterByName) == null) {
                        throw new InvalidOperationException(String.format(
                                "Post aggregation filtering on '%s' requires the field to be projected in the response",
                                projection.getAlias()));

                    //The column was projected but arguments didn't match.
                    } else {
                        throw new InvalidOperationException(String.format(
                                "Post aggregation filtering on '%s' requires the field to be projected "
                                        + "in the response with matching arguments",
                                projection.getAlias()));
                    }
                }
            });
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

            validatePredicate(query, predicate);
        });
    }

    protected void validatePredicate(Query query, FilterPredicate predicate) {
        SQLTable table = (SQLTable) query.getSource();
        Set<ColumnProjection> projections = extractFilterProjections(query, predicate);

        if (projections.isEmpty()) {
            return;
        }

        ColumnProjection projection = projections.iterator().next();

        validateColumn(query, projection);

        Column column = table.getColumn(Column.class, projection.getName());

        if (column.getValueType().equals(ValueType.ID)) {
            throw new InvalidOperationException("Filtering by ID is not supported on " + query.getSource().getName());
        }

        if (column.getValues() == null || column.getValues().isEmpty()) {
            return;
        }

        if (REGEX_OPERATORS.contains(predicate.getOperator())) {
            return;
        }

        predicate.getValues().forEach(value -> {
            if (! column.getValues().contains(value)) {
                throw new InvalidOperationException(String.format(
                        "Column '%s' values must match one of these values: %s",
                        projection.getAlias(),
                        column.getValues()));
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
                .collect(Collectors.toCollection(LinkedHashSet::new));

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

        query.getColumnProjections().forEach(column -> validateColumn(query, column));
    }

    @Override
    public void validateQueryArguments(Query query) {
        SQLTable table = (SQLTable) query.getSource();

        table.getArgumentDefinitions().forEach(tableArgument -> {
            Argument clientArgument = query.getArguments().get(tableArgument.getName());

            validateArgument(Optional.ofNullable(clientArgument), tableArgument,
                    "table '" + query.getSource().getName() + "'");
        });
    }

    protected void validateColumn(Query query, ColumnProjection projection) {
        validateColumnArguments(query, projection);
    }

    protected void validateColumnArguments(Query query, ColumnProjection projection) {
        SQLTable table = (SQLTable) query.getSource();
        Column column = table.getColumn(Column.class, projection.getName());

        column.getArgumentDefinitions().forEach(columnArgument -> {
            Argument clientArgument = projection.getArguments().get(columnArgument.getName());

            validateArgument(Optional.ofNullable(clientArgument), columnArgument,
                    "column '" + projection.getAlias() + "'");
        });
    }

    protected void validateArgument(
            Optional<Argument> clientArgument,
            ArgumentDefinition argumentDefinition,
            String context
    ) {

        if (! clientArgument.isPresent()) {
            if (argumentDefinition.isRequired()) {
                throw new InvalidOperationException(String.format("Argument '%s' for %s is required",
                        argumentDefinition.getName(),
                        context
                ));
            }
            return;
        }

        if (argumentDefinition.getValues() != null && ! argumentDefinition.getValues().isEmpty()) {
            if (! argumentDefinition.getValues().contains(clientArgument.get().getValue().toString())) {
                throw new InvalidOperationException(String.format(
                        "Argument '%s' for %s must match one of these values: %s",
                        argumentDefinition.getName(),
                        context,
                        argumentDefinition.getValues()));
            }
        }

        Object clientArgumentValue = clientArgument.get().getValue();
        boolean isValid = argumentDefinition.getType().matches(clientArgumentValue.toString());

        if (!isValid) {
            throw new InvalidOperationException(String.format("Argument '%s' for %s has an invalid value: %s",
                    argumentDefinition.getName(),
                    context,
                    clientArgumentValue));
        }
    }
}
