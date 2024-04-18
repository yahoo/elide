/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.datastores.aggregation.annotation;

import com.paiondata.elide.datastores.aggregation.metadata.enums.TimeGrain;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.TimeZone;

/**
 * Indicates that the annotated entity field is a temporal field and is backed by a temporal column in persistent
 * storage.
 */
@Documented
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Temporal {

    /**
     * The time grain supported by this time dimension.
     *
     * @return time grain.
     */
    TimeGrainDefinition[] grains() default {
        @TimeGrainDefinition(grain = TimeGrain.DAY, expression = "{{$$column.expr}}")
    };

    /**
     * The timezone in {@link String} of the column.
     * <p>
     * The String format can be expressed by
     * <ul>
     *     <li> an abbreviation such as "PST", or
     *     <li> a full name such as "America/Los_Angeles", or
     *     <li> a custom ID such as "GMT-8:00"
     * </ul>
     * The timezone will be parsed using {@link TimeZone#getTimeZone(String)}.
     *
     * @return data timezone
     */
    String timeZone() default "UTC";
}
