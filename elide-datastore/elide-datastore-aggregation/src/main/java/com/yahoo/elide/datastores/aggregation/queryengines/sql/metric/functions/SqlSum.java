/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql.metric.functions;

import com.yahoo.elide.datastores.aggregation.queryengines.sql.metric.SimpleSQLMetricFunction;

/**
 * Sum of a field.
 */
public class SqlSum extends SimpleSQLMetricFunction {
    public SqlSum() {
        super("sum", "sum", "sql sum function");
    }

    @Override
    public String getSQLExpression() {
        return "SUM(%metric)";
    }
}
