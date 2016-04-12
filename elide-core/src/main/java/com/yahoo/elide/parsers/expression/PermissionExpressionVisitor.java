/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.parsers.expression;

import com.yahoo.elide.core.CheckInstantiator;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.generated.parsers.ExpressionBaseVisitor;
import com.yahoo.elide.generated.parsers.ExpressionParser;
import com.yahoo.elide.security.checks.Check;
import com.yahoo.elide.security.permissions.expressions.AndExpression;
import com.yahoo.elide.security.permissions.expressions.Expression;
import com.yahoo.elide.security.permissions.expressions.NotExpression;
import com.yahoo.elide.security.permissions.expressions.OrExpression;

import java.util.function.Function;

/**
 * Expression Visitor.
 */
public class PermissionExpressionVisitor extends ExpressionBaseVisitor<Expression> implements CheckInstantiator {
    private final EntityDictionary dictionary;
    private final Function<Check, Expression> expressionGenerator;


    public PermissionExpressionVisitor(EntityDictionary dictionary, Function<Check, Expression> expressionGenerator) {
        this.dictionary = dictionary;
        this.expressionGenerator = expressionGenerator;
    }


    @Override
    public Expression visitNOT(ExpressionParser.NOTContext ctx) {
        // Create a not expression
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
        Check check = getCheck(dictionary, ctx.getText());

        return expressionGenerator.apply(check);
    }
}
