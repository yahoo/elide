/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql.metric.functions;

import com.yahoo.elide.datastores.aggregation.queryengines.sql.metric.SQLMetricFunction;

import java.util.Collections;

/**
 * Average of a field.
 */
public class SqlAvg extends SQLMetricFunction {
    public SqlAvg() {
        super("avg", "sql average function", "AVG(%s)", Collections.emptySet());
    }
}
