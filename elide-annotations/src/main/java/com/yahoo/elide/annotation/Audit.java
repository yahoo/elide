/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.annotation;

import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Audit configuration annotation.
 */
@Target({METHOD, FIELD, TYPE, PACKAGE})
@Retention(RUNTIME)
@Repeatable(Audits.class)
@Inherited
public @interface Audit {

    /**
     * Action performed type.
     */
    enum Action {
        /**
         * The CREATE.
         */
        CREATE,
        /**
         * The DELETE.
         */
        DELETE,
        /**
         * The UPDATE.
         */
        UPDATE
    }

    /**
     * Action performed.
     *
     * @return the action
     */
    Action action();

    /**
     * Regular expression applied to the path segments of the URI.  The audit only occurs if the expression
     * is empty or it matches the request URI
     * @return the string
     */
    String path() default "";

    /**
     * Operation code.
     *
     * @return the int
     */
    int operation();

    /**
     * Logging string template.
     * @return the string
     */
    String logStatement();

    /**
     * Unified expression language expressions that will be evaluated and substituted into the logging template.
     * @return the string [ ]
     */
    String [] logExpressions();
}
