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
 * Pre-delete hook. This annotation marks a callback that is triggered when a user performs a "delete" action.
 * This hook will be triggered <em>after</em> all security checks have been run, but <em>before</em> the datastore
 * has been committed.
 *
 * The invoked function takes a RequestScope as parameter.
 * @see com.yahoo.elide.security.RequestScope
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface OnDeletePreCommit {

}
