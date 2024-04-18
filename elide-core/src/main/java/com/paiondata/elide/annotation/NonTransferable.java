/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.annotation;

import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Marks that the given entity cannot be added to another collection after creation of the entity.
 */
@Target({TYPE, PACKAGE})
@Retention(RUNTIME)
@Inherited
public @interface NonTransferable {

    /**
     * If NonTransferable is used at the package level, it can be disabled for individual entities by setting
     * this flag to false.
     * @return true if enabled.
     */
    boolean enabled() default true;

    /**
     * Non-strict allows nested object hierarchies of non-transferables that are created in more than one
     * client request.  A non-transferable, A, can have a relationship updated post creation IF:
     *  - Another non-transferable, B, is being added to it.
     *  - A is the request lineage of B.  For example, /A/1/B is the request path.
     */
    boolean strict() default false;
}
