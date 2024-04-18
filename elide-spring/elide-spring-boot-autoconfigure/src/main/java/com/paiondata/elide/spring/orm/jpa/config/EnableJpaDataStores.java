/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.spring.orm.jpa.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to configure multiple JpaDataStores.
 *
 * @see com.paiondata.elide.datastores.jpa.JpaDataStore
 * @see com.paiondata.elide.spring.orm.jpa.config.EnableJpaDataStore
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface EnableJpaDataStores {
    EnableJpaDataStore[] value();
}
