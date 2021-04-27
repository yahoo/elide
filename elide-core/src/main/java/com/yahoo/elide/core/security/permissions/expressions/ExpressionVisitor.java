/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.core.security.permissions.expressions;

public interface ExpressionVisitor<T> {
    T visitExpression(Expression expression);
    T visitCheckExpression(CheckExpression checkExpression);
    T visitAndExpression(AndExpression andExpression);
    T visitOrExpression(OrExpression orExpression);
    T visitNotExpression(NotExpression notExpression);
}
