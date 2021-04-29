/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.core.security.visitors;

import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.security.RequestScope;
import com.yahoo.elide.core.security.checks.Check;
import com.yahoo.elide.core.security.checks.DatastoreEvalFilterExpressionCheck;
import com.yahoo.elide.core.security.checks.FilterExpressionCheck;
import com.yahoo.elide.core.security.checks.UserCheck;
import com.yahoo.elide.core.security.permissions.expressions.AndExpression;
import com.yahoo.elide.core.security.permissions.expressions.CheckExpression;
import com.yahoo.elide.core.security.permissions.expressions.Expression;
import com.yahoo.elide.core.security.permissions.expressions.ExpressionVisitor;
import com.yahoo.elide.core.security.permissions.expressions.NotExpression;
import com.yahoo.elide.core.security.permissions.expressions.OrExpression;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Objects;
import java.util.function.Function;

/**
 * Traverse the Expression Parser and returns the Permission Expression and boolean Pair.
 */
public class InmemoryPermissionExpressionVisitor implements ExpressionVisitor<Pair<Expression, Boolean>> {

    public static final Pair<Expression, Boolean> TRUE_EXPRESSION_PAIR =
            new ImmutablePair<>(Expression.Results.SUCCESS, false);

    public static final Pair<Expression, Boolean> FALSE_EXPRESSION_PAIR =
            new ImmutablePair<>(Expression.Results.FAILURE, false);

    private final EntityDictionary dictionary;
    private final RequestScope requestScope;
    private final Function<Check, Expression> expressionGenerator;
    private ExecuteFilterCheckInMemoryVisitor executeFilterCheckInMemoryVisitor;

    public InmemoryPermissionExpressionVisitor(EntityDictionary dictionary, RequestScope requestScope,
                                               Function<Check, Expression> expressionGenerator) {
        this.dictionary = dictionary;
        this.requestScope = requestScope;
        this.expressionGenerator = expressionGenerator;
        executeFilterCheckInMemoryVisitor = new ExecuteFilterCheckInMemoryVisitor();
    }

    @Override
    public Pair<Expression, Boolean> visitNotExpression(NotExpression notExpression) {
        Pair<Expression, Boolean> expressionPair = notExpression.getLogical().accept(this);
        if (Objects.equals(expressionPair, TRUE_EXPRESSION_PAIR)) {
            return FALSE_EXPRESSION_PAIR;
        }
        if (Objects.equals(expressionPair, FALSE_EXPRESSION_PAIR)) {
            return TRUE_EXPRESSION_PAIR;
        }

        if (expressionPair.getKey() instanceof CheckExpression) {
            CheckExpression checkExpression = (CheckExpression) expressionPair.getKey();
            if (checkExpression.getCheck() instanceof DatastoreEvalFilterExpressionCheck) {
                ((DatastoreEvalFilterExpressionCheck) checkExpression.getCheck()).negate();
                return expressionPair;
            }
        }

        return new ImmutablePair<>(new NotExpression(expressionPair.getKey()), expressionPair.getValue());
    }

    @Override
    public Pair<Expression, Boolean> visitOrExpression(OrExpression orExpression) {
        Pair<Expression, Boolean> leftPair = orExpression.getLeft().accept(this);
        Pair<Expression, Boolean> rightPair = orExpression.getRight().accept(this);
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
    public Pair<Expression, Boolean> visitAndExpression(AndExpression andExpression) {
        Pair<Expression, Boolean> leftPair = andExpression.getLeft().accept(this);
        Pair<Expression, Boolean> rightPair = andExpression.getRight().accept(this);

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
    public Pair<Expression, Boolean> visitExpression(Expression expression) {
        return FALSE_EXPRESSION_PAIR;
    }


    @Override
    public Pair<Expression, Boolean> visitCheckExpression(CheckExpression checkExpression) {
        Check check = checkExpression.getCheck();
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
