/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.annotation;

import com.yahoo.elide.security.checks.InlineCheck;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Assign custom Delete permission checks.
 */
@Target({TYPE, PACKAGE})
@Retention(RUNTIME)
public @interface DeletePermission {

    /**
     * Any one of these checks must pass.
     *
     * @return the array of check classes
     * @deprecated as of 2.2, use {@link #expression()} instead.
     */
    @Deprecated
    Class<? extends InlineCheck>[] any() default {};

    /**
     * All of these checks must pass.
     *
     * @return the array of check classes
     * @deprecated as of 2.2, use {@link #expression()} instead.
     */
    @Deprecated
    Class<? extends InlineCheck>[] all() default {};

    /**
     * An expression of checks that will be parsed via ANTLR. For example:
     * {@code @DeletePermission(expression="Prefab.Role.All")} or
     * {@code @DeletePermission(expression="Prefab.Role.All and Prefab.Role.UpdateOnCreate")}
     *
     * All of {@linkplain com.yahoo.elide.security.checks.prefab the built-in checks} are name-spaced as
     * {@code Prefab.CHECK} without the {@code Check} suffix
     *
     * @return the expression string to be parsed
     */
    String expression() default "";
}
