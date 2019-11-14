/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql.metric.functions;

import com.yahoo.elide.datastores.aggregation.queryengines.sql.metric.SimpleSQLMetricFunction;

/**
 * Average of a field.
 */
public class SqlAvg extends SimpleSQLMetricFunction {
    public SqlAvg() {
        super("avg", "average", "sql average function");
    }

    @Override
    public String getSQLExpression() {
        return "AVG(%metric)";
    }
}
