/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates whether a dimension has small, medium, or large cardinality.
 * <p>
 * Example: {@literal @}Cardinality(size = {@link CardinalitySize#MEDIUM}). If {@code size} is not specified,
 * {@link CardinalitySize#LARGE} is used by default. See {@link CardinalitySize}.
 * <p>
 * In the case of double binding, the following precedence rule is applied:
 * <ol>
 *     <li> {@link ElementType#TYPE}
 *     <li> {@link ElementType#METHOD} or {@link ElementType#FIELD}
 * </ol>
 */
@Documented
@Target({ TYPE, FIELD, METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface Cardinality {

    /**
     * Returns the size category of a dimension.
     * <p>
     * The size category must be from one of the values of type {@link CardinalitySize}. {@link CardinalitySize#LARGE}
     * will be the default if size is not specified.
     *
     * @return dimension size
     */
    CardinalitySize size() default CardinalitySize.LARGE;
}
