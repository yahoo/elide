/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.datastores.aggregation.annotation;

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
     * Join ON clause constraint for customizing relationship joins. {{..}} is used for column references.
     *
     * @return join constraint like <code>{{col1}} = {{joinField.col2}}</code>
     */
    String value();

    /**
     * Join type.
     * @return join type like {@code left, inner, full or cross}
     */
    JoinType type() default JoinType.LEFT;

    /**
     * Whether this joins to one or to many rows.
     * @return true if it joins to one row.
     */
    boolean toOne() default false;
}
