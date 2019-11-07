package com.yahoo.elide.datastores.aggregation.queryengines.sql.metric.functions;

import com.yahoo.elide.datastores.aggregation.queryengines.sql.metric.SimpleSQLMetricFunction;

import java.util.Collections;

public class SqlAvg extends SimpleSQLMetricFunction {
    public SqlAvg() {
        super("avg", "average", "sql average function");
    }
}
