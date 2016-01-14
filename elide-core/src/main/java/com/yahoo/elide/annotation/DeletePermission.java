/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.annotation;

import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import com.yahoo.elide.security.OperationCheck;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Assign custom Delete permission checks.
 */
@Target({TYPE, PACKAGE})
@Retention(RUNTIME)
@Inherited
public @interface DeletePermission {

    /**
     * Any one of these checks must pass.
     *
     * @return the class [ ]
     */
    Class<? extends OperationCheck>[] any() default {};

    /**
     * All of these checks must pass.
     *
     * @return the class [ ]
     */
    Class<? extends OperationCheck>[] all() default {};
}
