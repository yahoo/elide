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
 * Pre-read hook. This annotation marks a callback that is triggered when a user performs a "read" action.
 * This hook will be triggered <em>after</em> all security checks have been run, but <em>before</em> the datastore
 * has been committed.
 * <p>
 * The invoked function takes a RequestScope as parameter.
 *
 * @see com.yahoo.elide.security.RequestScope
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface OnReadPreCommit {
    /**
     * Field name on which the annotated method is only triggered if that field is modified.
     * If value is empty string, then trigger for any modification of the object.
     *
     * @return the field name that triggers the method
     */
    String value() default "";
}
