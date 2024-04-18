/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.spring.config;

import lombok.Data;

/**
 * Extra properties for setting up aggregation data store.
 */
@Data
public class JpaStoreProperties {

    /**
     * When fetching a subcollection from another multi-element collection, whether or not to do sorting, filtering
     * and pagination in memory - or do N+1 queries.
     */
    private boolean delegateToInMemoryStore = true;
}
