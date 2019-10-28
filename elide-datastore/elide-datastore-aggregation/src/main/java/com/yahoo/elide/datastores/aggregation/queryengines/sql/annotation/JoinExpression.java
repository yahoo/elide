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
 * JoinExpression is an arbitrary sql expression string with "%s" mark as a place holder for table aliases.
 * It would be used to build an expression following <code>from op joinTo</code> format, e.g.
 * <code>tb1.col1=tb2.col2</code> when we set from="%s.col1", op="=", joinTo="%s.col2"
 */
@Documented
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface JoinExpression {
    String from();
    String op() default "=";
    String joinTo();
}
