/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.spring.config;

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
     * {@link SQLDialect} type for default DataSource Object.
     */
    private String defaultDialect = "Hive";
}
