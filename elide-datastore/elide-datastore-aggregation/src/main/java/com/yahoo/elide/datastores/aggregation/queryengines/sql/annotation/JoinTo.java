/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the annotated entity field is derived from a join to another table.
 * This annotation must be present for relationship to views.
 */
@Documented
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface JoinTo {

    /**
     * Dot separated path through the entity relationship graph to an attribute.
     * If the current entity is author, then a path would be "book.publisher.name".
     * @return The path
     */
    String path() default "";

    /**
     * Join on clause constraint for customizing relationship joins as a plain sql string. Provided in the model.
     * Use "%from" and "%join% to represent the two sides of join.
     *
     * @return join constraint like <code>%from.col1 = %join.col2</code>
     */
    String joinClause() default "";
}
