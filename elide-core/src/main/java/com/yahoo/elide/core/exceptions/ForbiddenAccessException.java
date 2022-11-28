/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.exceptions;

import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.security.permissions.expressions.Expression;
import com.yahoo.elide.core.type.ClassType;

import lombok.Getter;

import java.lang.annotation.Annotation;
import java.util.Optional;

/**
 * Access to the requested resource is.
 *
 * {@link HttpStatus#SC_FORBIDDEN forbidden}
 */
public class ForbiddenAccessException extends HttpStatusException {
    private static final long serialVersionUID = 1L;

    @Getter private final Optional<Expression> expression;
    @Getter private final Optional<Expression.EvaluationMode> evaluationMode;

    public ForbiddenAccessException(Class<? extends Annotation> permission) {
        this(permission, null, null);
    }

    public ForbiddenAccessException(Class<? extends Annotation> permission,
                                    Expression expression, Expression.EvaluationMode mode) {
        super(HttpStatus.SC_FORBIDDEN, getMessage(permission), null, () -> getMessage(permission) + ": " + expression);

        this.expression = Optional.ofNullable(expression);
        this.evaluationMode = Optional.ofNullable(mode);
    }

    public String getLoggedMessage() {
        return String.format("ForbiddenAccessException: Message=%s\tMode=%s\tExpression=[%s]",
                             getVerboseMessage(), getEvaluationMode(), getExpression());
    }

    private static String getMessage(Class<? extends Annotation> permission) {
        return EntityDictionary.getSimpleName(ClassType.of(permission)) + " Denied";
    }
}
