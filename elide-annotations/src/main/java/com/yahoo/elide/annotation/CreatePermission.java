/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.annotation;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Assign custom Create permission checks.
 */
@Target({TYPE, PACKAGE, METHOD, FIELD})
@Retention(RUNTIME)
@Inherited
public @interface CreatePermission {

    /**
     * An expression of checks that will be parsed via ANTLR. For example:
     * {@code @CreatePermission(expression="Prefab.Role.All")} or
     * {@code @CreatePermission(expression="Prefab.Role.All and Prefab.Role.UpdateOnCreate")}
     *
     * All of {@linkplain com.yahoo.elide.security.checks.prefab the built-in checks} are name-spaced as
     * {@code Prefab.CHECK} without the {@code Check} suffix
     *
     * @return the expression string to be parsed
     */
    String expression() default "";
}
