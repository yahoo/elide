/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.core.security.visitors;

import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.security.CheckInstantiator;
import com.yahoo.elide.core.security.RequestScope;
import com.yahoo.elide.core.security.checks.Check;
import com.yahoo.elide.core.security.checks.DatastoreEvalFilterExpressionCheck;
import com.yahoo.elide.core.security.checks.FilterExpressionCheck;
import com.yahoo.elide.core.security.checks.UserCheck;
import com.yahoo.elide.core.security.permissions.expressions.AndExpression;
import com.yahoo.elide.core.security.permissions.expressions.Expression;
import com.yahoo.elide.core.security.permissions.expressions.NotExpression;
import com.yahoo.elide.core.security.permissions.expressions.OrExpression;
import com.yahoo.elide.generated.parsers.ExpressionBaseVisitor;
import com.yahoo.elide.generated.parsers.ExpressionParser;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Objects;
import java.util.function.Function;

/**
 * Traverse the Expression Parser and returns the Permission Expression and boolean Pair.
 */
public class InmemoryPermissionExpressionVisitor extends ExpressionBaseVisitor<Pair<Expression, Boolean>>
        implements CheckInstantiator {

    public static final Pair<Expression, Boolean> TRUE_EXPRESSION_PAIR = new ImmutablePair<>(Expression.Results.SUCCESS, false);

    public static final Pair<Expression, Boolean> FALSE_EXPRESSION_PAIR = new ImmutablePair<>(Expression.Results.FAILURE, false);

    private final EntityDictionary dictionary;
    private final RequestScope requestScope;
    private final Function<Check, Expression> expressionGenerator;
    private ExecuteFilterCheckInMemoryVisitor executeFilterCheckInMemoryVisitor;

    public InmemoryPermissionExpressionVisitor(EntityDictionary dictionary, RequestScope requestScope, Function<Check, Expression> expressionGenerator) {
        this.dictionary = dictionary;
        this.requestScope = requestScope;
        this.expressionGenerator = expressionGenerator;
        executeFilterCheckInMemoryVisitor = new ExecuteFilterCheckInMemoryVisitor();
    }

    @Override
    public Pair<Expression, Boolean> visitNOT(ExpressionParser.NOTContext ctx) {
        Pair<Expression, Boolean> expressionPair = visit(ctx.expression());
        if (Objects.equals(expressionPair, TRUE_EXPRESSION_PAIR)) {
            return FALSE_EXPRESSION_PAIR;
        }
        if (Objects.equals(expressionPair, FALSE_EXPRESSION_PAIR)) {
            return TRUE_EXPRESSION_PAIR;
        }

        return new ImmutablePair<>(new NotExpression(expressionPair.getKey()), expressionPair.getValue());
    }

    @Override
    public Pair<Expression, Boolean> visitOR(ExpressionParser.ORContext ctx) {
        Pair<Expression, Boolean> leftPair = visit(ctx.left);
        Pair<Expression, Boolean> rightPair = visit(ctx.right);
        if (Objects.equals(leftPair, TRUE_EXPRESSION_PAIR) || Objects.equals(rightPair, FALSE_EXPRESSION_PAIR)) {
            return leftPair;
        }
        if (Objects.equals(rightPair, TRUE_EXPRESSION_PAIR) || Objects.equals(leftPair, FALSE_EXPRESSION_PAIR)) {
            return rightPair;
        }

        Expression left = leftPair.getKey();
        Expression right = rightPair.getKey();
        if (leftPair.getValue()) {
            //inmemory check present in left. Change any FilterCheckwrapper to run inmemory in right
            right = right.accept(executeFilterCheckInMemoryVisitor);

        }
        if (rightPair.getValue()) {
            //inmemory check present in right. Change any FilterCheckwrapper to run inmemory in right
            left = left.accept(executeFilterCheckInMemoryVisitor);
        }

        return new ImmutablePair<>(new OrExpression(left, right),
                leftPair.getValue() || rightPair.getValue());
    }

    @Override
    public Pair<Expression, Boolean> visitAND(ExpressionParser.ANDContext ctx) {
        Pair<Expression, Boolean> leftPair = visit(ctx.left);
        Pair<Expression, Boolean> rightPair = visit(ctx.right);

        if (Objects.equals(leftPair, FALSE_EXPRESSION_PAIR) || Objects.equals(rightPair, FALSE_EXPRESSION_PAIR)) {
            return FALSE_EXPRESSION_PAIR;
        }

        if (Objects.equals(leftPair, TRUE_EXPRESSION_PAIR)) {
            return rightPair;
        }
        if (Objects.equals(rightPair, TRUE_EXPRESSION_PAIR)) {
            return leftPair;
        }

        return new ImmutablePair<>(new AndExpression(leftPair.getKey(), rightPair.getKey()),
                leftPair.getValue() || rightPair.getValue());
    }


    @Override
    public Pair<Expression, Boolean> visitPAREN(ExpressionParser.PARENContext ctx) {
        return visit(ctx.expression());
    }


    @Override
    public Pair<Expression, Boolean> visitPermissionClass(ExpressionParser.PermissionClassContext ctx) {
        Check check = getCheck(dictionary, ctx.getText());
        if (check instanceof FilterExpressionCheck) {
            return new ImmutablePair<>(expressionGenerator.apply(
                    new DatastoreEvalFilterExpressionCheck((FilterExpressionCheck) check)), false);
        }

        if (check instanceof UserCheck) {
            boolean userCheckResult = ((UserCheck) check).ok(requestScope.getUser());
            return userCheckResult ? TRUE_EXPRESSION_PAIR : FALSE_EXPRESSION_PAIR;
        }

        //Contains Operation check that has to be evaluated inmemory
        return new ImmutablePair<>(expressionGenerator.apply(check), true);
    }

}
