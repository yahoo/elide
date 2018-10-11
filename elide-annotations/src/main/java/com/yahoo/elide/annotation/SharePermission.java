/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.annotation;

import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * A permission that is checked whenever an object is loaded without the context of a lineage and assigned
 * to a relationship or collection. If SharePermission is specified, checking SharePermission falls back to checking
 * ReadPermission. Otherwise, the entity is not shareable.
 */
@Target({TYPE, PACKAGE})
@Retention(RUNTIME)
@Inherited
public @interface SharePermission {

    /**
     * A boolean value indicating if the entity is shareable. If not specifying, shareable is true. Setting shareable to
     * false provide a way to override package level SharePermission.
     *
     * @return the boolean if entity is shareable
     */
    boolean sharable() default true;
}
