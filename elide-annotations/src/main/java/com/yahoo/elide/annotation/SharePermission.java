/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.annotation;

import com.yahoo.elide.security.checks.InlineCheck;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * A permission that is checked whenever an object is loaded without the context of a lineage and assigned
 * to a relationship or collection.
 */
@Target({TYPE, PACKAGE})
@Retention(RUNTIME)
@Inherited
public @interface SharePermission {

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
