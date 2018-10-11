/*
 * Copyright 2015, Yahoo Inc.
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
 * Allows access to given entity.
 */
@Target({TYPE, PACKAGE})
@Retention(RUNTIME)
@Inherited
public @interface Include {

    /**
     * (Optional) Whether or not the entity can be accessed at the root URL path (i.e. /company)
     * @return the boolean
     */
    boolean rootLevel() default false;

    /**
     * The type of the JsonApi object. Defaults to the simple name of the entity class.
     * @return the string
     */
    String type() default "";
}
