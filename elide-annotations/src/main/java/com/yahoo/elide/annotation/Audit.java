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
     * @return the action (default = {Action.CREATE, Action.UPDATE, Action.DELETE})
     */
    Action[] action() default {Action.CREATE, Action.UPDATE, Action.DELETE};

    /**
     * Regular expression applied to the path segments of the URI.  The audit only occurs if the expression
     * is empty or it matches the request URI
     * @return the string (default = "")
     */
    String path() default "";

    /**
     * Operation code.
     *
     * @return the int (default = -1)
     */
    int operation() default -1;

    /**
     * Logging string template.
     * @return the string (default = "")
     */
    String logStatement() default "";

    /**
     * Unified expression language expressions that will be evaluated and substituted into the logging template.
     * @return the string [ ] (default = "")
     */
    String [] logExpressions() default "";
}
