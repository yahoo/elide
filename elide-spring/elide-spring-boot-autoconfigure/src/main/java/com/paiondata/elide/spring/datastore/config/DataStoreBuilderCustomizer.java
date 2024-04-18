/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.spring.datastore.config;

/**
 * Used to customize the DataStoreBuilder.
 *
 * @see DataStoreBuilder
 */
public interface DataStoreBuilderCustomizer {
    void customize(DataStoreBuilder dataStoreBuilder);
}
