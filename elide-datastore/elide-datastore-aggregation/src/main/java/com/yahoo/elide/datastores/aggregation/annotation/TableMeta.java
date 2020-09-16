/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the specified entity field has a configured long name and field description for human to read on UI.
 */
@Documented
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface TableMeta {

    String description() default "";
    String category() default "";

    String [] tags() default {};

    /**
     * Whether or not querying this table requires a client provided filter.
     * @return The required filter template.
     */
    String filterTemplate() default "";
}
