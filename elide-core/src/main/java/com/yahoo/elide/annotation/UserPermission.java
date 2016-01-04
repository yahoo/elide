/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.annotation;

import com.yahoo.elide.optimization.UserCheck;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation providing a user-level permission set on an entity. This permission takes precedence over all others
 * and can be used to short-circuit potentially expensive permissions in future check types.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface UserPermission {
    /**
     * Any one of these checks must pass.
     *
     * @return the class [ ]
     */
    Class<? extends UserCheck>[] any() default {};

    /**
     * All of these checks must pass.
     *
     * @return the class [ ]
     */
    Class<? extends UserCheck>[] all() default {};
}
