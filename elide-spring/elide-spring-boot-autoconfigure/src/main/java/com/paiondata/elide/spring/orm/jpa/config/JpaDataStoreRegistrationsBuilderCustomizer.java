/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.spring.orm.jpa.config;

/**
 * Customizer to customize registration entries for building JpaDataStores.
 *
 * @see com.paiondata.elide.spring.orm.jpa.config.JpaDataStoreRegistrationsBuilder
 */
@FunctionalInterface
public interface JpaDataStoreRegistrationsBuilderCustomizer {
    void customize(JpaDataStoreRegistrationsBuilder builder);
}
