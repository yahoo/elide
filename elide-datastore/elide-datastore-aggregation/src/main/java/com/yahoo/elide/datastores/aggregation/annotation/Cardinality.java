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
 * Indicates whether a dimension has small, medium, or large cardinality.
 * <p>
 * See {@link DimensionSize}. By default, the size is set to {@link DimensionSize#LARGE}.
 * <p>
 * Example: {@literal @}Cardinality(size = {@link DimensionSize#MEDIUM})
 */
@Documented
@Target({ElementType.TYPE, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Cardinality {

    /**
     * Returns the size category of a dimension.
     * <p>
     * The size category must be from one of the values of type {@link DimensionSize}. {@link DimensionSize#LARGE} will
     * be the default if size is not specified.
     *
     * @return dimension size
     */
    DimensionSize size() default DimensionSize.LARGE;
}
