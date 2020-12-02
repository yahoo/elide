/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.modelconfig.validator;

import com.yahoo.elide.core.security.permissions.expressions.AndExpression;
import com.yahoo.elide.core.security.permissions.expressions.Expression;
import com.yahoo.elide.core.security.permissions.expressions.NotExpression;
import com.yahoo.elide.core.security.permissions.expressions.OrExpression;
import com.yahoo.elide.generated.parsers.ExpressionBaseVisitor;
import com.yahoo.elide.generated.parsers.ExpressionParser;

import lombok.AllArgsConstructor;

import java.util.function.Function;

/**
 * Expression Visitor.
 */
@AllArgsConstructor
public class PermissionExpressionVisitor extends ExpressionBaseVisitor<Expression> {
    private final Function<String, Expression> expressionGenerator;

    @Override
    public Expression visitNOT(ExpressionParser.NOTContext ctx) {
        return new NotExpression(visit(ctx.expression()));
    }

    @Override
    public Expression visitOR(ExpressionParser.ORContext ctx) {
        return new OrExpression(visit(ctx.left), visit(ctx.right));
    }

    @Override
    public Expression visitAND(ExpressionParser.ANDContext ctx) {
        Expression left = visit(ctx.left);
        Expression right = visit(ctx.right);
        return new AndExpression(left, right);
    }

    @Override
    public Expression visitPAREN(ExpressionParser.PARENContext ctx) {
        return visit(ctx.expression());
    }

    @Override
    public Expression visitPermissionClass(ExpressionParser.PermissionClassContext ctx) {
        return expressionGenerator.apply(ctx.getText());
    }
}
