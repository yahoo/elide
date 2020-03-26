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
 * Indicates that the annotated entity dimension field is derived from a join to another table.
 * The other table can be a relationship entity or {@link Join} entity.
 */
@Documented
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface JoinTo {
    /**
     * Dot separated path through the entity relationship graph to an attribute starting for current table.
     * e.g. current entity is author, a path would be "book.publisher.name" where "book" is a relationship/join table.
     *
     * @return The path
     */
    String path() default "";
}
