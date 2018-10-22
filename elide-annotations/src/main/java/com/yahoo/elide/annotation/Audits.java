/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Audit configuration annotation repeatable.
 */
@Retention(RUNTIME)
@Target({METHOD, FIELD, TYPE})
@Inherited
public @interface Audits {

    /**
     * the repeatable Audit annotation.
     *
     * @return the audit [ ]
     */
    Audit [] value();
}
