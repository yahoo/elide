/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Used to annotate interfaces that represent mapped entities.
 * Used for returning non related entities, for examaple from HQL or @Any mapped methods.
 */
@Target({TYPE})
@Retention(RUNTIME)
public @interface MappedInterface {
}
