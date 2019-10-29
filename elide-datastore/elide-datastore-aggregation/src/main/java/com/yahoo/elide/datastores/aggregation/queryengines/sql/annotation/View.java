/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * View indicates the name of this view and the sql expression to select it. Only for models that don't have id field.
 */
@Target(TYPE)
@Retention(RUNTIME)
public @interface View {
    /**
     * Name defined for this view.
     * @return view name
     */
    String name() default "";

    /**
     * Whether this view is from a table directly
     * @return True if the view is from a table
     */
    boolean isTable() default true;

    /**
     * Table name or select statement to construct this view.
     * @return sql statement
     */
    String from();
}
