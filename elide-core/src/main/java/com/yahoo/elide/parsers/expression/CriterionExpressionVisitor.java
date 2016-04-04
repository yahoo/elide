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
import com.yahoo.elide.security.RequestScope;
import com.yahoo.elide.security.checks.Check;
import com.yahoo.elide.security.checks.CriterionCheck;

import java.util.function.BiFunction;
import java.util.function.Function;


/**
 * Returns a Criterion (user specified type) from a security expression.
 * @param <T> the return type of the CriterionCheck
 */
public class CriterionExpressionVisitor<T> extends ExpressionBaseVisitor<T> implements CheckInstantiator {


    private final RequestScope requestScope;
    private final EntityDictionary entityDictionary;
    private final Function<T, T> criterionNegater;
    private final BiFunction<T, T, T> orCriterionJoiner;
    private final BiFunction<T, T, T> andCriterionJoiner;

    public CriterionExpressionVisitor(RequestScope requestScope,
                                      EntityDictionary entityDictionary,
                                      Function<T, T> criterionNegater,
                                      BiFunction<T, T, T> orCriterionJoiner,
                                      BiFunction<T, T, T> andCriterionJoiner) {
        this.requestScope = requestScope;
        this.entityDictionary = entityDictionary;

        this.criterionNegater = criterionNegater;
        this.orCriterionJoiner = orCriterionJoiner;
        this.andCriterionJoiner = andCriterionJoiner;
    }

    @Override
    public T visitNOT(ExpressionParser.NOTContext ctx) {
        T criterion = visit(ctx);

        if (criterion != null) {
            criterion = criterionNegater.apply(criterion);
        }

        return criterion;
    }

    @Override
    public T visitOR(ExpressionParser.ORContext ctx) {
        return joinSubexpressions(ctx.left, ctx.right, orCriterionJoiner);
    }

    @Override
    public T visitAND(ExpressionParser.ANDContext ctx) {
        return joinSubexpressions(ctx.left, ctx.right, andCriterionJoiner);
    }

    @Override
    public T visitPermissionClass(ExpressionParser.PermissionClassContext ctx) {
        CriterionCheck<T, ?> criterionCheck = getCriterionCheck(ctx.getText());

        if (criterionCheck == null) {
            return null;
        }

        return criterionCheck.getCriterion(requestScope);
    }

    private T joinSubexpressions(ExpressionParser.ExpressionContext left,
                                 ExpressionParser.ExpressionContext right,
                                 BiFunction<T, T, T> joiner) {
        T leftCriterion = visit(left);
        T rightCriterion = visit(right);

        if (leftCriterion == null && rightCriterion == null) {
            return null;

        } else if (leftCriterion == null) {
            return rightCriterion;

        } else if (rightCriterion == null) {
            return leftCriterion;
        }

        return joiner.apply(leftCriterion, rightCriterion);
    }

    private CriterionCheck<T, ?> getCriterionCheck(String checkName) {
        Check check = getCheck(entityDictionary, checkName);

        return check instanceof CriterionCheck
                ? (CriterionCheck<T, ?>) check
                : null;

    }
}
