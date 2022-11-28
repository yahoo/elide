/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.core.security.visitors;

import com.yahoo.elide.core.security.permissions.expressions.AndExpression;
import com.yahoo.elide.core.security.permissions.expressions.AnyFieldExpression;
import com.yahoo.elide.core.security.permissions.expressions.BooleanExpression;
import com.yahoo.elide.core.security.permissions.expressions.CheckExpression;
import com.yahoo.elide.core.security.permissions.expressions.Expression;
import com.yahoo.elide.core.security.permissions.expressions.ExpressionVisitor;
import com.yahoo.elide.core.security.permissions.expressions.NotExpression;
import com.yahoo.elide.core.security.permissions.expressions.OrExpression;
import com.yahoo.elide.core.security.permissions.expressions.SpecificFieldExpression;

/**
 * Expression Visitor to normalize Permission expression.
 */
public class PermissionExpressionNormalizationVisitor implements ExpressionVisitor<Expression> {
    @Override
    public Expression visitSpecificFieldExpression(SpecificFieldExpression expression) {
        return expression;
    }

    @Override
    public Expression visitAnyFieldExpression(AnyFieldExpression expression) {
        return expression;
    }

    @Override
    public Expression visitBooleanExpression(BooleanExpression expression) {
        return expression;
    }

    @Override
    public Expression visitCheckExpression(CheckExpression checkExpression) {
        return checkExpression;
    }

    @Override
    public Expression visitAndExpression(AndExpression andExpression) {
        Expression left = andExpression.getLeft();
        Expression right = andExpression.getRight();
        return new AndExpression(left.accept(this), right.accept(this));
    }

    @Override
    public Expression visitOrExpression(OrExpression orExpression) {
        Expression left = orExpression.getLeft();
        Expression right = orExpression.getRight();
        return new OrExpression(left.accept(this), right.accept(this));
    }

    @Override
    public Expression visitNotExpression(NotExpression notExpression) {
        Expression inner = notExpression.getLogical();
        if (inner instanceof AndExpression) {
            AndExpression and = (AndExpression) inner;
            Expression left = new NotExpression(and.getLeft()).accept(this);
            Expression right = new NotExpression(and.getRight()).accept(this);
            return new OrExpression(left, right);
        }
        if (inner instanceof OrExpression) {
            OrExpression or = (OrExpression) inner;
            Expression left = new NotExpression(or.getLeft()).accept(this);
            Expression right = new NotExpression(or.getRight()).accept(this);
            return new AndExpression(left, right);
        }
        if (inner instanceof NotExpression) {
            NotExpression not = (NotExpression) inner;
            return (not.getLogical()).accept(this);
        }
        return notExpression;
    }
}
