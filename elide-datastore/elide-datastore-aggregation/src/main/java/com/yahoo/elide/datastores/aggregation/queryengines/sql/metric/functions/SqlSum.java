/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql.metric.functions;

import com.yahoo.elide.datastores.aggregation.metadata.metric.AggregatableField;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metric.BasicSQLMetricFunction;
import com.yahoo.elide.request.Argument;

import java.util.List;
import java.util.Map;

/**
 * Sum of a field.
 */
public class SqlSum extends BasicSQLMetricFunction {
    public SqlSum() {
        super("sum", "sum", "sql sum function");
    }

    protected String buildSQL(Map<String, Argument> arguments, List<AggregatableField> fields) {
        return String.format("SUM(%s)", fields.get(0).getName());
    }
}
