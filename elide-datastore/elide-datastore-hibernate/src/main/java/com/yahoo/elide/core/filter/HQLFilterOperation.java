/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.filter;

import com.yahoo.elide.core.exceptions.InvalidPredicateException;
import com.yahoo.elide.core.exceptions.InvalidValueException;
import com.yahoo.elide.core.filter.FilterPredicate.FilterParameter;
import com.yahoo.elide.core.filter.expression.AndFilterExpression;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.expression.FilterExpressionVisitor;
import com.yahoo.elide.core.filter.expression.NotFilterExpression;
import com.yahoo.elide.core.filter.expression.OrFilterExpression;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * FilterOperation that creates Hibernate query language fragments.
 */
public class HQLFilterOperation implements FilterOperation<String> {
    private static final String FILTER_PATH_NOT_NULL = "Filtering field path cannot be empty.";
    private static final String FILTER_ALIAS_NOT_NULL = "Filtering alias cannot be empty.";
    public static final String PARAM_JOIN = ", ";
    public static final Function<FilterParameter, String> LOWERED_PARAMETER = p ->
            String.format("lower(%s)", p.getPlaceholder());

    @Override
    public String apply(FilterPredicate filterPredicate) {
        return apply(filterPredicate, Optional.empty());
    }

    /**
     * Transforms a filter predicate into a HQL query fragment.
     * @param filterPredicate The predicate to transform.
     * @param aliasProvider Function which supplies an alias to append to the predicate fields.
     *                      This is useful for table aliases referenced in HQL for some kinds of joins.
     * @return The hql query fragment.
     */
    protected String apply(FilterPredicate filterPredicate, Optional<Function<FilterPredicate, String>> aliasProvider) {
        String fieldPath = filterPredicate.getFieldPath();

        if (aliasProvider.isPresent()) {
            fieldPath = aliasProvider.get().apply(filterPredicate) + "." + filterPredicate.getField();
        }

        //HQL doesn't support 'this', but it does support aliases.
        fieldPath = fieldPath.replaceAll("\\.this", "");

        List<FilterParameter> params = filterPredicate.getParameters();
        String firstParam = params.size() > 0 ? params.get(0).getPlaceholder() : null;
        switch (filterPredicate.getOperator()) {
            case IN:
                Preconditions.checkState(!filterPredicate.getValues().isEmpty());
                return String.format("%s IN (%s)", fieldPath, params.stream()
                        .map(FilterParameter::getPlaceholder)
                        .collect(Collectors.joining(PARAM_JOIN)));

            case IN_INSENSITIVE:
                Preconditions.checkState(!filterPredicate.getValues().isEmpty());
                return String.format("lower(%s) IN (%s)", fieldPath, params.stream()
                        .map(LOWERED_PARAMETER)
                        .collect(Collectors.joining(PARAM_JOIN)));

            case NOT:
                Preconditions.checkState(!filterPredicate.getValues().isEmpty());
                return String.format("%s NOT IN (%s)", fieldPath, params.stream()
                        .map(FilterParameter::getPlaceholder)
                        .collect(Collectors.joining(PARAM_JOIN)));

            case NOT_INSENSITIVE:
                Preconditions.checkState(!filterPredicate.getValues().isEmpty());
                return String.format("lower(%s) NOT IN (%s)", fieldPath, params.stream()
                        .map(LOWERED_PARAMETER)
                        .collect(Collectors.joining(PARAM_JOIN)));

            case PREFIX:
                return String.format("%s LIKE CONCAT(%s, '%%')", fieldPath, firstParam);

            case PREFIX_CASE_INSENSITIVE:
                assertValidValues(fieldPath, firstParam);
                return String.format("lower(%s) LIKE CONCAT(lower(%s), '%%')", fieldPath, firstParam);

            case POSTFIX:
                return String.format("%s LIKE CONCAT('%%', %s)", fieldPath, firstParam);

            case POSTFIX_CASE_INSENSITIVE:
                assertValidValues(fieldPath, firstParam);
                return String.format("lower(%s) LIKE CONCAT('%%', lower(%s))", fieldPath, firstParam);

            case INFIX:
                return String.format("%s LIKE CONCAT('%%', %s, '%%')", fieldPath, firstParam);

            case INFIX_CASE_INSENSITIVE:
                assertValidValues(fieldPath, firstParam);
                return String.format("lower(%s) LIKE CONCAT('%%', lower(%s), '%%')", fieldPath, firstParam);

            case LT:
                return String.format("%s < %s", fieldPath, params.size() == 1 ? firstParam : leastClause(params));

            case LE:
                return String.format("%s <= %s", fieldPath, params.size() == 1 ? firstParam : leastClause(params));

            case GT:
                return String.format("%s > %s", fieldPath, params.size() == 1 ? firstParam : greatestClause(params));

            case GE:
                return String.format("%s >= %s", fieldPath, params.size() == 1 ? firstParam : greatestClause(params));

            // Not parametric checks
            case ISNULL:
                return String.format("%s IS NULL", fieldPath);

            case NOTNULL:
                return String.format("%s IS NOT NULL", fieldPath);

            case TRUE:
                return "(1 = 1)";

            case FALSE:
                return "(1 = 0)";

            default:
                throw new InvalidPredicateException("Operator not implemented: " + filterPredicate.getOperator());
        }
    }

    private String greatestClause(List<FilterParameter> params) {
        return String.format("greatest(%s)", params.stream()
                .map(FilterParameter::getPlaceholder)
                .collect(Collectors.joining(PARAM_JOIN)));
    }

    private String leastClause(List<FilterParameter> params) {
        return String.format("least(%s)", params.stream()
                .map(FilterParameter::getPlaceholder)
                .collect(Collectors.joining(PARAM_JOIN)));
    }

    private void assertValidValues(String fieldPath, String alias) {
        if (Strings.isNullOrEmpty(fieldPath)) {
            throw new InvalidValueException(FILTER_PATH_NOT_NULL);
        }
        if (Strings.isNullOrEmpty(alias)) {
            throw new IllegalStateException(FILTER_ALIAS_NOT_NULL);
        }
    }

    /**
     * Translates the filterExpression to a JPQL filter fragment.
     * @param filterExpression The filterExpression to translate
     * @param prefixWithAlias If true, use the default alias provider to append the predicates with an alias.
     *                        Otherwise, don't append aliases.
     * @return A JPQL filter fragment.
     */
    public String apply(FilterExpression filterExpression, boolean prefixWithAlias) {
        Optional<Function<FilterPredicate, String>> aliasProvider = Optional.empty();
        if (prefixWithAlias) {
            aliasProvider = Optional.of((predicate) -> predicate.getAlias());
        }

        return apply(filterExpression, aliasProvider);
    }

    /**
     * Translates the filterExpression to a JPQL filter fragment.
     * @param filterExpression The filterExpression to translate
     * @param aliasProvider Optionally appends the predicates clause with an alias.
     * @return A JPQL filter fragment.
     */
    public String apply(FilterExpression filterExpression, Optional<Function<FilterPredicate, String>> aliasProvider) {
        HQLQueryVisitor visitor = new HQLQueryVisitor(aliasProvider);
        return "WHERE " + filterExpression.accept(visitor);
    }

    /**
     * Filter expression visitor which builds an HQL query.
     */
    public class HQLQueryVisitor implements FilterExpressionVisitor<String> {
        public static final String TWO_NON_FILTERING_EXPRESSIONS =
                "Cannot build a filter from two non-filtering expressions";
        private Optional<Function<FilterPredicate, String>> aliasProvider;

        public HQLQueryVisitor(Optional<Function<FilterPredicate, String>> aliasProvider) {
            this.aliasProvider = aliasProvider;
        }

        @Override
        public String visitPredicate(FilterPredicate filterPredicate) {
            return apply(filterPredicate, aliasProvider);
        }

        @Override
        public String visitAndExpression(AndFilterExpression expression) {
            FilterExpression left = expression.getLeft();
            FilterExpression right = expression.getRight();
            return "(" + left.accept(this) + " AND " + right.accept(this) + ")";
        }

        @Override
        public String visitOrExpression(OrFilterExpression expression) {
            FilterExpression left = expression.getLeft();
            FilterExpression right = expression.getRight();
            return "(" + left.accept(this) + " OR " + right.accept(this) + ")";
        }

        @Override
        public String visitNotExpression(NotFilterExpression expression) {
            String negated = expression.getNegated().accept(this);
            return "NOT (" + negated + ")";
        }
    }
}
