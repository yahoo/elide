/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * On Read trigger annotation.
 */
@Target({METHOD, FIELD})
@Retention(RUNTIME)
public @interface OnRead {
    /**
     * Field name to be read to trigger hook. If unspecified, invoked on <em>any</em> read.
     *
     * Trigger is invoked immediately before field is read.
     */
    String value() default "";
}
