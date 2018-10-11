/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

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
