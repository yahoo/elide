/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Allows pagination options to be set with querying of an entity.
 */
@Target({TYPE})
@Retention(RUNTIME)
@Inherited
public @interface Paginate {

    /**
     * Whether or not page totals can be requested.
     * @return the boolean
     */
    boolean countable() default true;

    /**
     * For limiting the number of entities which can be returned from a query.
     * @return the default limit
     */
    int defaultLimit() default 500;

    /**
     * For setting an upper bound on the limit that can be specified as request parameter when fetching entities.
     * @return the maximum limit
     */
    int maxLimit() default 10000;
}
