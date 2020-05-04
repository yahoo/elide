/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql.metric.functions;

import com.yahoo.elide.datastores.aggregation.queryengines.sql.metric.SQLMetricFunction;

import java.util.Collections;

/**
 * Sum of a field.
 */
public class SqlSum extends SQLMetricFunction {
    public SqlSum() {
        super("sum", "sql sum function", "SUM(%s)", Collections.emptySet());
    }
}
