/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.security.permissions.expressions;

import com.yahoo.elide.security.permissions.ExpressionResult;

import static com.yahoo.elide.security.permissions.ExpressionResult.PASS_RESULT;
import static com.yahoo.elide.security.permissions.ExpressionResult.Status.FAIL;

/**
 * Implementation of joining expression results by any field success or entity success.
 *
 * Specifically, this expression determines whether a permission is available at _any_ level of annotation
 * for an object. That is, if the object has sufficient privilege at the package-, entity-, or field-level
 * then this check will return PASS.
 */
public class AnyFieldExpression implements Expression {
    private final Expression entityExpression;
    private final OrExpression fieldExpression;

    public AnyFieldExpression(final Expression entityExpression, final OrExpression fieldExpression) {
        this.entityExpression = entityExpression;
        this.fieldExpression = fieldExpression;
    }

    @Override
    public ExpressionResult evaluate() {
        ExpressionResult fieldResult = (fieldExpression == null) ? new ExpressionResult(FAIL, "Invalid expression")
                                                                 : fieldExpression.evaluate();
        if (fieldResult.getStatus() != FAIL) {
            return fieldResult;
        }
        return (entityExpression == null) ? PASS_RESULT : entityExpression.evaluate();
    }
}
