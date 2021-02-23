/*
 * Copyright 2020, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.standalone.config;

import com.yahoo.elide.datastores.aggregation.cache.CaffeineCache;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.SQLDialect;
import com.yahoo.elide.modelconfig.DBPasswordExtractor;
import com.yahoo.elide.modelconfig.model.DBConfig;
import org.apache.commons.lang3.StringUtils;

import java.io.File;

/**
 * Interface for configuring the Analytic configuration of Standalone Application.
 */
public interface ElideStandaloneAnalyticSettings {

    /**
     * Enable the support for Dynamic Model Configuration. If false, the feature will be disabled. If enabled, ensure
     * that Aggregation Data Store is also enabled
     *
     * @return Default: False
     */
    default boolean enableDynamicModelConfig() {
        return false;
    }

    /**
     * Base path to Hjson dynamic model configurations.
     *
     * @return Default: /configs/
     */
    default String getDynamicConfigPath() {
        return File.separator + "configs" + File.separator;
    }

    /**
     * Enable the support for Aggregation Data Store. If false, the feature will be disabled.
     *
     * @return Default: False
     */
    default boolean enableAggregationDataStore() {
        return false;
    }

    /**
     * Enable the support for MetaData Store. If false, the feature will be disabled.
     *
     * @return Default: False
     */
    default boolean enableMetaDataStore() {
        return false;
    }

    /**
     * Provides the default SQLDialect type.
     *
     * @return {@link SQLDialect} type for default DataSource Object.
     */
    default String getDefaultDialect() {
        return "Hive";
    }

    /**
     * Creates the default Password Extractor Implementation.
     *
     * @return An instance of DBPasswordExtractor.
     */
    default DBPasswordExtractor getDBPasswordExtractor() {
        return new DBPasswordExtractor() {
            @Override
            public String getDBPassword(DBConfig config) {
                return StringUtils.EMPTY;
            }
        };
    }

    /**
     * Limit on number of query cache entries. Non-positive values disable the query cache.
     *
     * @return Default: 1024
     */
    default Integer getQueryCacheMaximumEntries() {
        return CaffeineCache.DEFAULT_MAXIMUM_ENTRIES;
    }

    /**
     * Returns the default expiration in minutes of items in the AggregationDataStore query cache.
     *
     * @return Default: 10
     */
    default Long getDefaultCacheExpirationMinutes() {
        return 10L;
    }
}
