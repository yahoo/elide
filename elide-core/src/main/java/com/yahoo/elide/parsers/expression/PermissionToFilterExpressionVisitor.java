/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.parsers.expression;

import com.yahoo.elide.core.CheckInstantiator;
import com.yahoo.elide.core.EntityDictionary;
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
    private final RequestScope requestScope;

    public static final FilterExpression NO_EVALUATION_EXPRESSION = new FilterExpression() {
        @Override
        public <T> T accept(Visitor<T> visitor) {
            return null;
        }
    };
    public static final FilterExpression USER_CHECK_EXPRESSION = new FilterExpression() {
        @Override
        public <T> T accept(Visitor<T> visitor) {
            return null;
        }
    };

    public PermissionToFilterExpressionVisitor(EntityDictionary dictionary, RequestScope requestScope) {
        this.dictionary = dictionary;
        this.requestScope = requestScope;
    }

    @Override
    public FilterExpression visitOR(ExpressionParser.ORContext ctx) {
        FilterExpression left = visit(ctx.left);
        FilterExpression right = visit(ctx.right);

        if (left == NO_EVALUATION_EXPRESSION || right == NO_EVALUATION_EXPRESSION) {
            return NO_EVALUATION_EXPRESSION;
        }

        //This special case require future reconsideration.
        if (left == USER_CHECK_EXPRESSION || right == USER_CHECK_EXPRESSION) {
            return USER_CHECK_EXPRESSION;
        }

        return new OrFilterExpression(left, right);
    }

    @Override
    public FilterExpression visitPermissionClass(ExpressionParser.PermissionClassContext ctx) {
        Check check = getCheck(dictionary, ctx.getText());
        if (FilterExpressionCheck.class.isAssignableFrom(check.getClass())) {
            FilterExpression filterExpression = ((FilterExpressionCheck) check).getFilterExpression(requestScope);
            if (filterExpression == null) {
                throw new IllegalStateException("FilterExpression null is not permitted.");
            }
            return ((FilterExpressionCheck) check).getFilterExpression(requestScope);
        } else if (UserCheck.class.isAssignableFrom(check.getClass())) {
            return USER_CHECK_EXPRESSION;
        } else {
            return NO_EVALUATION_EXPRESSION;
        }
    }

    @Override
    public FilterExpression visitAND(ExpressionParser.ANDContext ctx) {
        FilterExpression left = visit(ctx.left);
        FilterExpression right = visit(ctx.right);

        // (NO_EVA and UserCheck) should return NO_EVA let the upper level know.
        // For example, FilterExpressionCheck Or (NO_EVA and UserCheck) should evaluate to  NO_EVALUATION_EXPRESSION.
        if (left == NO_EVALUATION_EXPRESSION && right == USER_CHECK_EXPRESSION) {
            return left;
        }

        if (right == NO_EVALUATION_EXPRESSION && left == USER_CHECK_EXPRESSION) {
            return right;
        }

        if (left == NO_EVALUATION_EXPRESSION || left == USER_CHECK_EXPRESSION) {
            return right;
        }

        if (right == NO_EVALUATION_EXPRESSION || right == USER_CHECK_EXPRESSION) {
            return left;
        }
        return new AndFilterExpression(left, right);
    }

    @Override
    public FilterExpression visitPAREN(ExpressionParser.PARENContext ctx) {
        return visit(ctx.expression());
    }
}
