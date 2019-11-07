package com.yahoo.elide.datastores.aggregation.queryengines.sql.metric.functions;

import com.yahoo.elide.datastores.aggregation.queryengines.sql.metric.SimpleSQLMetricFunction;

import java.util.Collections;

public class SqlSum extends SimpleSQLMetricFunction {
    public SqlSum() {
        super("sum", "sum", "sql sum function");
    }

    @Override
    public String getExpression() {
        return "SUM(%metric)";
    }
}
