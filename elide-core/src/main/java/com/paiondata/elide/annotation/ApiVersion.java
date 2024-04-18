/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.annotation;

import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Versions API Models.
 */
@Target({PACKAGE})
@Retention(RUNTIME)
public @interface ApiVersion {

    /**
     * Models in this package are tied to this API version.
     * @return the string (default = "")
     */
    String version() default "";
}
