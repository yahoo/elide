/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.spring.config;

import static com.yahoo.elide.datastores.aggregation.cache.CaffeineCache.DEFAULT_MAXIMUM_ENTRIES;

import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.SQLDialect;

import lombok.Data;

/**
 * Extra properties for setting up aggregation data store.
 */
@Data
public class AggregationStoreProperties {

    /**
     * Whether or not aggregation data store is enabled.
     */
    private boolean enabled = false;

    /**
     * Whether or not meta data store is enabled.
     */
    private boolean enableMetaDataStore = false;

    /**
     * {@link SQLDialect} type for default DataSource Object.
     */
    private String defaultDialect = "Hive";

    /**
     * Limit on number of query cache entries. Non-positive values disable the query cache.
     */
    private int queryCacheMaximumEntries = DEFAULT_MAXIMUM_ENTRIES;

    /**
     * Default Cache Expiration.
     */
    private long defaultCacheExpirationMinutes = 10;
}
