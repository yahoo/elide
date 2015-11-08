/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Enables an <b>existing</b>entity to be be added to an existing relation or collection.
 */
@Target(TYPE)
@Retention(RUNTIME)
public @interface Shareable {
}
