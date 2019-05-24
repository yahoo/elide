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
 * Indicates that a field is computed via a {@link #expression() JPQL metric formula expression}.
 * <p>
 * Example: {@literal @}ComputedMetric(expression = '(fieldA * fieldB) / 100')
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MetricComputation {

    /**
     * The JPQL expression for that represents this metric computation logic.
     *
     * @return metric formula
     */
    String expression();
}
