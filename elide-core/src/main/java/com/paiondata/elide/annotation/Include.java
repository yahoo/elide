/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.annotation;

import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Allows access to given entity.
 */
@Target({TYPE, PACKAGE})
@Retention(RUNTIME)
public @interface Include {

    /**
     * (Optional) Whether or not the entity can be accessed at the root URL path (i.e. /company).
     * @return the boolean
     */
    boolean rootLevel() default true;

    /**
     * When annotating a type, the name of the model.  When unset, the model name defaults to the
     * simple name of the entity class.
     *
     * When annotation a package, the prefix to apply (prefix_modelName) to apply to each model in the package.
     * @return the string
     */
    String name() default "";

    /**
     * The model or package description.
     * @return the string
     */
    String description() default "";

    /**
     * The model or package friendly name (for display).
     * @return the string
     */
    String friendlyName() default "";
}
