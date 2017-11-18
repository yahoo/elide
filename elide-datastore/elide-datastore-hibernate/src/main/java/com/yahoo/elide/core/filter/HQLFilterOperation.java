/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.filter;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.yahoo.elide.core.exceptions.InvalidPredicateException;
import com.yahoo.elide.core.exceptions.InvalidValueException;
import com.yahoo.elide.core.filter.FilterPredicate.FilterParameter;
import com.yahoo.elide.core.filter.expression.AndFilterExpression;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.expression.FilterExpressionVisitor;
import com.yahoo.elide.core.filter.expression.NotFilterExpression;
import com.yahoo.elide.core.filter.expression.OrFilterExpression;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.yahoo.elide.parsers.expression.PermissionToFilterExpressionVisitor.FALSE_USER_CHECK_EXPRESSION;
import static com.yahoo.elide.parsers.expression.PermissionToFilterExpressionVisitor.NO_EVALUATION_EXPRESSION;

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
        return apply(filterPredicate, false);
    }

    /**
     * Transforms a filter predicate into a HQL query fragment.
     * @param filterPredicate The predicate to transform.
     * @param prefixWithAlias Whether or not to append the entity type to the predicate.
     *                       This is useful for table aliases referenced in HQL for some kinds of joins.
     * @return The hql query fragment.
     */
    protected String apply(FilterPredicate filterPredicate, boolean prefixWithAlias) {
        String fieldPath = filterPredicate.getFieldPath();

        if (prefixWithAlias) {
            fieldPath = filterPredicate.getAlias() + "." + filterPredicate.getField();
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

    public String apply(FilterExpression filterExpression, boolean prefixWithAlias) {
        HQLQueryVisitor visitor = new HQLQueryVisitor(prefixWithAlias);
        return "WHERE " + filterExpression.accept(visitor);

    }

    /**
     * Filter expression visitor which builds an HQL query.
     */
    public class HQLQueryVisitor implements FilterExpressionVisitor<String> {
        private boolean prefixWithAlias;

        public HQLQueryVisitor(boolean prefixWithAlias) {
            this.prefixWithAlias = prefixWithAlias;
        }

        @Override
        public String visitPredicate(FilterPredicate filterPredicate) {
            return apply(filterPredicate, prefixWithAlias);
        }

        @Override
        public String visitAndExpression(AndFilterExpression expression) {
            FilterExpression left = expression.getLeft();
            FilterExpression right = expression.getRight();
            boolean skipLeft = cannotEvaluate(left);
            boolean skipRight = cannotEvaluate(right);
            if (skipLeft && skipRight) {
                throw new IllegalStateException("Cannot build a filter from two non-filtering expressions");
            } else if (skipLeft) {
                return right.accept(this);
            } else if (skipRight) {
                return left.accept(this);
            }
            return "(" + left.accept(this) + " AND " + right.accept(this) + ")";
        }

        @Override
        public String visitOrExpression(OrFilterExpression expression) {
            FilterExpression left = expression.getLeft();
            FilterExpression right = expression.getRight();
            boolean skipLeft = cannotEvaluate(left);
            boolean skipRight = cannotEvaluate(right);
            if (skipLeft && skipRight) {
                throw new IllegalStateException("Cannot build a filter from two non-filtering expressions");
            } else if (skipLeft) {
                return right.accept(this);
            } else if (skipRight) {
                return left.accept(this);
            }
            return "(" + left.accept(this) + " OR " + right.accept(this) + ")";
        }

        private boolean cannotEvaluate(FilterExpression expression) {
            return expression == NO_EVALUATION_EXPRESSION || expression == FALSE_USER_CHECK_EXPRESSION;
        }

        @Override
        public String visitNotExpression(NotFilterExpression expression) {
            String negated = expression.getNegated().accept(this);
            return "NOT (" + negated + ")";
        }
    }
}
