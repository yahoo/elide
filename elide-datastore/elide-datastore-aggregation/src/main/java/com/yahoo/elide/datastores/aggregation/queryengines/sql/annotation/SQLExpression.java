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
 * Provide arbitrary sql expression on dimension field or method.
 * This expression should use '%reference' as the place holder for physical table column reference.
 * i.e. "IF(%reference >= 0, 'positive', 'negative')".
 *
 * Sql expression can only be applied on degenerated dimension field or physical column reference.
 * Using this annotation on metric field is invalid as metric function expression already has the same functionality.
 * Using this annotation on relationship field is invalid as relationship field is not physical table column.
 * Sql expression can be carried when used with @JoinTo, the outer layer expression would be applied after the initial
 * joined to sql expression.
 */
@Documented
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface SQLExpression {
    String value();
}
