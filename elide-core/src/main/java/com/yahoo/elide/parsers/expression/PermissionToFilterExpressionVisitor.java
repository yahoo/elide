/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.parsers.expression;

import com.yahoo.elide.core.CheckInstantiator;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.filter.FilterPredicate;
import com.yahoo.elide.core.filter.Operator;
import com.yahoo.elide.core.filter.expression.AndFilterExpression;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.expression.FilterExpressionVisitor;
import com.yahoo.elide.core.filter.expression.NotFilterExpression;
import com.yahoo.elide.core.filter.expression.OrFilterExpression;
import com.yahoo.elide.generated.parsers.ExpressionBaseVisitor;
import com.yahoo.elide.generated.parsers.ExpressionParser;
import com.yahoo.elide.security.FilterExpressionCheck;
import com.yahoo.elide.security.RequestScope;
import com.yahoo.elide.security.checks.Check;
import com.yahoo.elide.security.checks.UserCheck;

import java.util.Objects;


/**
 * PermissionToFilterExpressionVisitor parses a permission parseTree and returns the corresponding FilterExpression
 * representation of it. This allows passing a security permission predicate down to datastore level to reduce
 * in-memory permission verification workload.
 * A few cases is not allow and will throw exception:
 *      1. User define FilterExpressionCheck which returns null in getFilterExpression function.
 *      2. User put a FilterExpressionCheck with a non-userCheck type check in OR relation.
 */
public class PermissionToFilterExpressionVisitor extends ExpressionBaseVisitor<FilterExpression>
        implements CheckInstantiator {
    private final EntityDictionary dictionary;
    private final Class entityClass;
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
            Class entityClass) {
        this.dictionary = dictionary;
        this.requestScope = requestScope;
        this.entityClass = entityClass;
    }

    @Override
    public FilterExpression visitNOT(ExpressionParser.NOTContext ctx) {
        FilterExpression expression = visit(ctx.expression());
        if (Objects.equals(expression, TRUE_USER_CHECK_EXPRESSION)) {
            return FALSE_USER_CHECK_EXPRESSION;
        } else if (Objects.equals(expression, FALSE_USER_CHECK_EXPRESSION)) {
            return TRUE_USER_CHECK_EXPRESSION;
        } else if (Objects.equals(expression, NO_EVALUATION_EXPRESSION)) {
            return NO_EVALUATION_EXPRESSION;
        }
        return new NotFilterExpression(expression);
    }

    @Override
    public FilterExpression visitOR(ExpressionParser.ORContext ctx) {
        FilterExpression left = visit(ctx.left);
        FilterExpression right = visit(ctx.right);

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
    public FilterExpression visitAND(ExpressionParser.ANDContext ctx) {
        FilterExpression left = visit(ctx.left);
        FilterExpression right = visit(ctx.right);

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
    public FilterExpression visitPermissionClass(ExpressionParser.PermissionClassContext ctx) {
        Check check = getCheck(dictionary, ctx.getText());
        if (check instanceof FilterExpressionCheck) {
            FilterExpressionCheck filterCheck = (FilterExpressionCheck) check;
            FilterExpression filterExpression = filterCheck.getFilterExpression(entityClass, requestScope);

            if (filterExpression == null) {
                throw new IllegalStateException("FilterCheck#getFilterExpression must not return null.");
            }

            return filterExpression;
        }

        if (UserCheck.class.isAssignableFrom(check.getClass())) {
            boolean userCheckResult = check.ok(requestScope.getUser());
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
    public FilterExpression visitPAREN(ExpressionParser.PARENContext ctx) {
        return visit(ctx.expression());
    }
}
