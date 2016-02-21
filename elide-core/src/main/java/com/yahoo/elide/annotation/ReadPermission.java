/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import com.yahoo.elide.security.checks.InlineCheck;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Assign custom Read permission checks.
 */
@Target({METHOD, FIELD, TYPE, PACKAGE})
@Retention(RUNTIME)
@Inherited
public @interface ReadPermission {

    /**
     * Any one of these checks must pass.
     *
     * @return the class [ ]
     */
    Class<? extends InlineCheck>[] any() default {};

    /**
     * All of these checks must pass.
     *
     * @return the class [ ]
     */
    Class<? extends InlineCheck>[] all() default {};

    /**
     * An expression of checks that will be parsed via ANTLR.
     * @return the expression string to be parsed
     */
    String expression() default "";
}
