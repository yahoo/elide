/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.datastores.aggregation.metadata.models.Metric;

/**
 * Overrides the construction of metric functions for the SQL query engine.
 */
public class SQLMetric extends Metric {
    public SQLMetric(SQLTable table, String fieldName, EntityDictionary dictionary) {
        super(table, fieldName, dictionary);
    }
}
