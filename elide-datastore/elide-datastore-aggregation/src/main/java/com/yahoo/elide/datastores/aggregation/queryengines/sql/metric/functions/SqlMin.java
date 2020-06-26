/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql.metric.functions;

import com.yahoo.elide.datastores.aggregation.queryengines.sql.metric.SQLMetricFunction;

import java.util.Collections;

/**
 * Min of a field.
 */
public class SqlMin extends SQLMetricFunction {
    public SqlMin() {
        super("min", "sql min function", "sql function", "MIN(%s)", Collections.emptySet());
    }
}
