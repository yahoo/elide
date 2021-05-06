/*
 * Copyright 2021, Yahoo Inc.
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
 * Indicates that the specified namespace has a configured long name and field description for human to read on UI.
 */
@Documented
@Target({ElementType.PACKAGE})
@Retention(RetentionPolicy.RUNTIME)
public @interface NamespaceMeta {

    String friendlyName() default "";
    String description() default "";
}
