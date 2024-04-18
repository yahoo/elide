/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.datastores.aggregation.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the specified entity field has a configured long name and field description for human to read on UI.
 */
@Documented
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ColumnMeta {
    String friendlyName() default "";
    String description() default "";
    String category() default "";
    TableSource tableSource() default @TableSource(table = "", column = "");
    String [] tags() default {};
    String [] values() default {};

    /**
     * Controls whether this column is exposed through the Metadata store.
     * @return true or false.
     */
    boolean isHidden() default false;

    /**
     * Whether or not querying this column requires a client provided filter.
     * @return The required filter template.
     */
    String filterTemplate() default "";

    /**
     * Indicates the cardinality for the column.
     * @return size
     */
    CardinalitySize size() default CardinalitySize.UNKNOWN;
}
