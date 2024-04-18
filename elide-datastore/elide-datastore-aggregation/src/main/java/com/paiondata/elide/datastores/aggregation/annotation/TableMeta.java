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
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface TableMeta {

    String friendlyName() default "";
    String description() default "";
    String category() default "";

    String [] tags() default {};

    /**
     * Optimizer and query planning hints.
     * @return The list of supported hints.
     */
    String [] hints() default {};

    /**
     * Whether or not querying this table requires a client provided filter.
     * @return The required filter template.
     */
    String filterTemplate() default "";

    /**
     * Whether or not this table is a fact table.
     * @return true or false.
     */
    boolean isFact() default true;

    /**
     * Controls whether this table is exposed through the Metadata store.
     * @return true or false.
     */
    boolean isHidden() default false;

    /**
     * Indicates the size of the table.
     * @return size
     */
    CardinalitySize size() default CardinalitySize.UNKNOWN;

    /**
     * The arguments accepted by this table.
     * @return arguments for the table
     */
    ArgumentDefinition[] arguments() default {};
}
