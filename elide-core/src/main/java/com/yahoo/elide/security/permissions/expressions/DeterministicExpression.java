/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.security.permissions.expressions;


import com.yahoo.elide.security.permissions.ExpressionResult;

/**
 * Expression that only returns the value initially passed into it, but looks like an expression.
 */
public class DeterministicExpression implements Expression {
    private final ExpressionResult result;

    /**
     * Constructor.
     * <p>
     * Holds a single ExpressionResult with a predetermiend value in a wrapper to make it look like an Expression.
     * This makes walking the ExpressionVisitor easier as then all the expression visitor methods return an Expression.
     *
     * @param result The final expression result which can be returned up the tree.
     */
    public DeterministicExpression(final ExpressionResult result) {
        this.result = result;
    }

    @Override
    public ExpressionResult evaluate() {
        return this.result;
    }
}
