/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.parsers.expression;

import com.yahoo.elide.core.CheckInstantiator;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.filter.Operator;
import com.yahoo.elide.core.filter.FilterPredicate;
import com.yahoo.elide.core.filter.expression.AndFilterExpression;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.expression.OrFilterExpression;
import com.yahoo.elide.core.filter.expression.Visitor;
import com.yahoo.elide.generated.parsers.ExpressionBaseVisitor;
import com.yahoo.elide.generated.parsers.ExpressionParser;
import com.yahoo.elide.security.FilterExpressionCheck;
import com.yahoo.elide.security.RequestScope;
import com.yahoo.elide.security.checks.Check;
import com.yahoo.elide.security.checks.UserCheck;


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

    public static final FilterExpression NO_EVALUATION_EXPRESSION = new FilterExpression() {
        @Override
        public <T> T accept(Visitor<T> visitor) {
            return null;
        }
        @Override
        public String toString() {
            return "NO_EVALUATION_EXPRESSION";
        }
    };

    public static final FilterExpression FALSE_USER_CHECK_EXPRESSION = new FilterExpression() {
        @Override
        public <T> T accept(Visitor<T> visitor) {
            return null;
        }
        @Override
        public String toString() {
            return "FALSE_USER_CHECK_EXPRESSION";
        }
    };

    public PermissionToFilterExpressionVisitor(EntityDictionary dictionary, RequestScope requestScope,
            Class entityClass) {
        this.dictionary = dictionary;
        this.requestScope = requestScope;
        this.entityClass = entityClass;
    }

    @Override
    public FilterExpression visitOR(ExpressionParser.ORContext ctx) {
        FilterExpression left = visit(ctx.left);
        FilterExpression right = visit(ctx.right);

        // short circuit if TRUE
        if (operator(left) == Operator.TRUE) {
            return left;
        }

        // short circuit if TRUE
        if (operator(right) == Operator.TRUE) {
            return right;
        }

        if (left == NO_EVALUATION_EXPRESSION || right == NO_EVALUATION_EXPRESSION) {
            return NO_EVALUATION_EXPRESSION;
        }

        if (left == FALSE_USER_CHECK_EXPRESSION || operator(left) == Operator.FALSE) {
            return right;
        } else if (right == FALSE_USER_CHECK_EXPRESSION || operator(right) == Operator.FALSE) {
            return left;
        }

        return new OrFilterExpression(left, right);
    }

    @Override
    public FilterExpression visitPermissionClass(ExpressionParser.PermissionClassContext ctx) {
        Check check = getCheck(dictionary, ctx.getText());
        if (check instanceof FilterExpressionCheck) {
            FilterExpression filterExpression =
                    ((FilterExpressionCheck) check).getFilterExpression(entityClass, requestScope);
            if (filterExpression == null) {
                throw new IllegalStateException("FilterExpression null is not permitted.");
            }
            return filterExpression;
        } else if (UserCheck.class.isAssignableFrom(check.getClass())) {
            boolean userCheckResult = check.ok(requestScope.getUser());
            return userCheckResult ? NO_EVALUATION_EXPRESSION : FALSE_USER_CHECK_EXPRESSION;
        } else {
            return NO_EVALUATION_EXPRESSION;
        }
    }

    @Override
    public FilterExpression visitAND(ExpressionParser.ANDContext ctx) {
        FilterExpression left = visit(ctx.left);
        FilterExpression right = visit(ctx.right);

        //Case (FALSE_USER_CHECK_EXPRESSION AND FE):  should evaluate to FALSE_USER_CHECK_EXPRESSION
        //Case (FALSE_USER_CHECK_EXPRESSION AND NO_EVALUATION_EXPRESSION): should also evaluate to
        // FALSE_USER_CHECK_EXPRESSION
        if (left == FALSE_USER_CHECK_EXPRESSION || right == FALSE_USER_CHECK_EXPRESSION) {
            return FALSE_USER_CHECK_EXPRESSION;
        }

        if (operator(left) == Operator.FALSE) {
            return FALSE_USER_CHECK_EXPRESSION;
        }

        if (operator(right) == Operator.FALSE) {
            return FALSE_USER_CHECK_EXPRESSION;
        }

        //Case (NO_EVALUATION_EXPRESSION AND FilterExpression): should ignore NO_EVALUATION_EXPRESSION and return
        // FilterExpression.
        //Case (NO_EVALUATION_EXPRESSION AND NO_EVALUATION_EXPRESSION): returns NO_EVALUATION_EXPRESSION.
        if (left == NO_EVALUATION_EXPRESSION || operator(left) == Operator.TRUE) {
            return right;
        }

        if (right == NO_EVALUATION_EXPRESSION || operator(right) == Operator.TRUE) {
            return left;
        }

        //Case (FilterExpression AND FilterExpression): should return the AND expression of them.
        return new AndFilterExpression(left, right);
    }

    private Operator operator(FilterExpression expression) {
        if (expression instanceof FilterPredicate) {
            return ((FilterPredicate) expression).getOperator();
        }
        return null;
    }

    @Override
    public FilterExpression visitPAREN(ExpressionParser.PARENContext ctx) {
        return visit(ctx.expression());
    }
}
