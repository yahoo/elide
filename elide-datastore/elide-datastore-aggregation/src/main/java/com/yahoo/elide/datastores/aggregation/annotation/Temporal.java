/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.annotation;

import com.yahoo.elide.datastores.aggregation.time.TimeGrain;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the annotated entity field is a temporal field and is backed by a temporal column in persistent
 * storage.
 */
@Documented
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Temporal {

    /**
     * All supported units into which temporal column can be divided
     *
     * @return all allowed time grain of a persistent storage column
     */
    TimeGrain timeGrain();

    /**
     * The timezone of the column.
     *
     * @return data timezone
     */
    String timeZone();
}
