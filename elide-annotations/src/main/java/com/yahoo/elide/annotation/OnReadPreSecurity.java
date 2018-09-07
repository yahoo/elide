/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * On read trigger annotation.
 * <p>
 * The invoked function takes a RequestScope as parameter.
 *
 * @see com.yahoo.elide.security.RequestScope
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface OnReadPreSecurity {
    /**
     * Field name on which the annotated method is only triggered if that field is read.
     * If value is empty string, then trigger once when the object is read.
     * If value is "*", then trigger for all field reads.
     *
     * @return the field name that triggers this method
     */
    String value() default "";
}
