/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.core.security.visitors;

import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.filter.Operator;
import com.yahoo.elide.core.filter.expression.AndFilterExpression;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.expression.FilterExpressionVisitor;
import com.yahoo.elide.core.filter.expression.NotFilterExpression;
import com.yahoo.elide.core.filter.expression.OrFilterExpression;
import com.yahoo.elide.core.filter.predicates.FilterPredicate;
import com.yahoo.elide.core.security.RequestScope;
import com.yahoo.elide.core.security.checks.Check;
import com.yahoo.elide.core.security.checks.FilterExpressionCheck;
import com.yahoo.elide.core.security.checks.UserCheck;
import com.yahoo.elide.core.security.permissions.expressions.AndExpression;
import com.yahoo.elide.core.security.permissions.expressions.AnyFieldExpression;
import com.yahoo.elide.core.security.permissions.expressions.BooleanExpression;
import com.yahoo.elide.core.security.permissions.expressions.CheckExpression;
import com.yahoo.elide.core.security.permissions.expressions.ExpressionVisitor;
import com.yahoo.elide.core.security.permissions.expressions.NotExpression;
import com.yahoo.elide.core.security.permissions.expressions.OrExpression;
import com.yahoo.elide.core.security.permissions.expressions.SpecificFieldExpression;
import com.yahoo.elide.core.type.Type;

import java.util.Objects;


/**
 * PermissionToFilterExpressionVisitor parses a permission parseTree and returns the corresponding FilterExpression
 * representation of it. This allows passing a security permission predicate down to datastore level to reduce
 * in-memory permission verification workload.
 * A few cases is not allow and will throw exception:
 *      1. User define FilterExpressionCheck which returns null in getFilterExpression function.
 *      2. User put a FilterExpressionCheck with a non-userCheck type check in OR relation.
 */
public class PermissionToFilterExpressionVisitor implements ExpressionVisitor<FilterExpression> {
    private final EntityDictionary dictionary;
    private final Type entityClass;
    private final RequestScope requestScope;

    /**
     * This is a constant that represents a check that we cannot evaluate at extraction time.
     * Usually this is an inline or commit-time check.
     */
    public static final FilterExpression NO_EVALUATION_EXPRESSION = new FilterExpression() {
        @Override
        public <T> T accept(FilterExpressionVisitor<T> visitor) {
            return (T) this;
        }
        @Override
        public String toString() {
            return "NO_EVALUATION_EXPRESSION";
        }
    };

    /**
     * This represents a user check that has evaluated to false.
     */
    public static final FilterExpression FALSE_USER_CHECK_EXPRESSION = new FilterExpression() {
        @Override
        public <T> T accept(FilterExpressionVisitor<T> visitor) {
            return (T) this;
        }
        @Override
        public String toString() {
            return "FALSE_USER_CHECK_EXPRESSION";
        }
    };

    /**
     * This represents a user check that has evaluated to true.
     */
    public static final FilterExpression TRUE_USER_CHECK_EXPRESSION = new FilterExpression() {
        @Override
        public <T> T accept(FilterExpressionVisitor<T> visitor) {
            return (T) this;
        }
        @Override
        public String toString() {
            return "TRUE_USER_EXPRESSION";
        }
    };

    public PermissionToFilterExpressionVisitor(EntityDictionary dictionary, RequestScope requestScope,
            Type entityClass) {
        this.dictionary = dictionary;
        this.requestScope = requestScope;
        this.entityClass = entityClass;
    }

    @Override
    public FilterExpression visitNotExpression(NotExpression notExpression) {
        FilterExpression expression = notExpression.getLogical().accept(this);
        if (Objects.equals(expression, TRUE_USER_CHECK_EXPRESSION)) {
            return FALSE_USER_CHECK_EXPRESSION;
        }
        if (Objects.equals(expression, FALSE_USER_CHECK_EXPRESSION)) {
            return TRUE_USER_CHECK_EXPRESSION;
        }
        if (Objects.equals(expression, NO_EVALUATION_EXPRESSION)) {
            return NO_EVALUATION_EXPRESSION;
        }
        if (expression instanceof FilterPredicate) {
            return ((FilterPredicate) expression).negate();
        }
        return new NotFilterExpression(expression);
    }

    @Override
    public FilterExpression visitOrExpression(OrExpression orExpression) {
        FilterExpression left = orExpression.getLeft().accept(this);
        FilterExpression right = orExpression.getRight().accept(this);

        if (expressionWillNotFilter(left)) {
            return left;
        }

        if (expressionWillNotFilter(right)) {
            return right;
        }

        boolean leftFails = expressionWillFail(left);
        boolean rightFails = expressionWillFail(right);
        if (leftFails && rightFails) {
            return FALSE_USER_CHECK_EXPRESSION;
        }
        if (leftFails) {
            return right;
        }
        if (rightFails) {
            return left;
        }

        return new OrFilterExpression(left, right);
    }

    @Override
    public FilterExpression visitAndExpression(AndExpression andExpression) {
        FilterExpression left = andExpression.getLeft().accept(this);
        FilterExpression right = andExpression.getRight().accept(this);

        // (FALSE_USER_CHECK_EXPRESSION AND FilterExpression) => FALSE_USER_CHECK_EXPRESSION
        // (FALSE_USER_CHECK_EXPRESSION AND NO_EVALUATION_EXPRESSION) => FALSE_USER_CHECK_EXPRESSION
        if (expressionWillFail(left) || expressionWillFail(right)) {
            return FALSE_USER_CHECK_EXPRESSION;
        }

        if (expressionWillNotFilter(left)) {
            return right;
        }

        if (expressionWillNotFilter(right)) {
            return left;
        }

        return new AndFilterExpression(left, right);
    }

    private boolean expressionWillFail(FilterExpression expression) {
        return Objects.equals(expression, FALSE_USER_CHECK_EXPRESSION) || operator(expression) == Operator.FALSE;
    }

    private boolean expressionWillNotFilter(FilterExpression expression) {
        return Objects.equals(expression, NO_EVALUATION_EXPRESSION)
                || Objects.equals(expression, TRUE_USER_CHECK_EXPRESSION)
                || operator(expression) == Operator.TRUE;
    }

    @Override
    public FilterExpression visitCheckExpression(CheckExpression checkExpression) {
        Check check = checkExpression.getCheck();
        if (check instanceof FilterExpressionCheck) {
            FilterExpressionCheck filterCheck = (FilterExpressionCheck) check;
            FilterExpression filterExpression = filterCheck.getFilterExpression(entityClass, requestScope);

            if (filterExpression == null) {
                throw new IllegalStateException("FilterCheck#getFilterExpression must not return null.");
            }

            return filterExpression;
        }

        if (check instanceof UserCheck) {
            boolean userCheckResult = ((UserCheck) check).ok(requestScope.getUser());
            return userCheckResult ? TRUE_USER_CHECK_EXPRESSION : FALSE_USER_CHECK_EXPRESSION;
        }

        return NO_EVALUATION_EXPRESSION;
    }

    private Operator operator(FilterExpression expression) {
        return expression instanceof FilterPredicate
                ? ((FilterPredicate) expression).getOperator()
                : null;
    }

    @Override
    public FilterExpression visitSpecificFieldExpression(SpecificFieldExpression expression) {
        return NO_EVALUATION_EXPRESSION;
    }

    @Override
    public FilterExpression visitAnyFieldExpression(AnyFieldExpression expression) {
        return NO_EVALUATION_EXPRESSION;
    }

    @Override
    public FilterExpression visitBooleanExpression(BooleanExpression expression) {
        return NO_EVALUATION_EXPRESSION;
    }
}
