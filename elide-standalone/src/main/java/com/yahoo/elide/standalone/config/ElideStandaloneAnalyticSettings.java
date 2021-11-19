/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.standalone.config;

import com.yahoo.elide.core.security.RequestScope;
import com.yahoo.elide.core.security.User;
import com.yahoo.elide.core.security.checks.OperationCheck;
import com.yahoo.elide.core.security.checks.UserCheck;
import com.yahoo.elide.datastores.aggregation.cache.CaffeineCache;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.SQLDialect;
import com.yahoo.elide.modelconfig.DBPasswordExtractor;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.Optional;

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
     * Enable support for reading and manipulating HJSON configuration through Elide models.
     *
     * @return Default: False
     */
    default boolean enableDynamicModelConfigAPI() {
        return false;
    }

    default UserCheck getConfigApiReadCheck() {
        return new UserCheck() {
            @Override
            public boolean ok(User user) {
                return false;
            }
        };
    }

    default OperationCheck getConfigApiUpdateCheck() {
        return new OperationCheck() {
            @Override
            public boolean ok(Object object, RequestScope requestScope, Optional optional) {
                return false;
            }
        };
    }

    default OperationCheck getConfigApiDeleteCheck() {
        return new OperationCheck() {
            @Override
            public boolean ok(Object object, RequestScope requestScope, Optional optional) {
                return false;
            }
        };
    }

    default OperationCheck getConfigApiCreateCheck() {
        return new OperationCheck() {
            @Override
            public boolean ok(Object object, RequestScope requestScope, Optional optional) {
                return false;
            }
        };
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
        return config -> StringUtils.EMPTY;
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
