/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Join {
    /**
     * Join on clause constraint for customizing relationship joins as a plain sql string. Provided in the model.
     * Use "%from" and "%join% to represent the two sides of join.
     *
     * @return join constraint like <code>%from.col1 = %join.col2</code>
     */
    String value();
}
