/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.core.security.visitors;

import com.yahoo.elide.core.security.checks.Check;
import com.yahoo.elide.core.security.checks.DatastoreEvalFilterExpressionCheck;
import com.yahoo.elide.core.security.permissions.expressions.AndExpression;
import com.yahoo.elide.core.security.permissions.expressions.CheckExpression;
import com.yahoo.elide.core.security.permissions.expressions.Expression;
import com.yahoo.elide.core.security.permissions.expressions.ExpressionVisitor;
import com.yahoo.elide.core.security.permissions.expressions.NotExpression;
import com.yahoo.elide.core.security.permissions.expressions.OrExpression;

public class ExecuteFilterCheckInMemoryVisitor implements ExpressionVisitor<Expression> {

    @Override
    public Expression visitExpression(Expression expression) {
        return expression;
    }

    @Override
    public Expression visitCheckExpression(CheckExpression checkExpression) {
        Check check = checkExpression.getCheck();
        if (check instanceof DatastoreEvalFilterExpressionCheck) {
            ((DatastoreEvalFilterExpressionCheck) check).setExecutedInMemory(true);
        }
        return checkExpression;
    }

    @Override
    public Expression visitAndExpression(AndExpression andExpression) {
        andExpression.getLeft().accept(this);
        andExpression.getRight().accept(this);
        return andExpression;
    }

    @Override
    public Expression visitOrExpression(OrExpression orExpression) {
        orExpression.getLeft().accept(this);
        orExpression.getRight().accept(this);
        return orExpression;
    }

    @Override
    public Expression visitNotExpression(NotExpression notExpression) {
        notExpression.getLogical().accept(this);
        return notExpression;
    }
}
