/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.spring.config;

import static com.paiondata.elide.datastores.aggregation.cache.CaffeineCache.DEFAULT_MAXIMUM_ENTRIES;

import com.paiondata.elide.datastores.aggregation.queryengines.sql.dialects.SQLDialect;

import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.boot.convert.DurationUnit;

import lombok.Data;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

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
     * {@link SQLDialect} type for default DataSource Object.
     */
    private String defaultDialect = "Hive";

    /**
     * Settings for the Dynamic Configuration.
     */
    @NestedConfigurationProperty
    private DynamicConfigProperties dynamicConfig = new DynamicConfigProperties();

    @Data
    public static class MetadataStore {
        /**
         * Whether or not meta data store is enabled.
         */
        private boolean enabled;
    }

    private MetadataStore metadataStore = new MetadataStore();

    @Data
    public static class QueryCache {
        /**
         * Whether or not to enable the query cache.
         */
        private boolean enabled = true;

        /**
         * Limit on number of query cache entries.
         */
        private int maxSize = DEFAULT_MAXIMUM_ENTRIES;

        /**
         * Query cache expiration after write.
         */
        @DurationUnit(ChronoUnit.MINUTES)
        private Duration expiration = Duration.ofMinutes(10L);
    }

    private QueryCache queryCache = new QueryCache();

}
