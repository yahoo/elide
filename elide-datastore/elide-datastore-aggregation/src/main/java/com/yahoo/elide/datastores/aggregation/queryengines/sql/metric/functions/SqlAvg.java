/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql.metric.functions;

import com.yahoo.elide.datastores.aggregation.metadata.metric.AggregatableField;
import com.yahoo.elide.request.Argument;

import java.util.List;
import java.util.Map;

/**
 * Average of a field.
 */
public class SqlAvg extends BasicSQLMetricFunction {
    public SqlAvg() {
        super("avg", "average", "sql average function");
    }

    @Override
    protected String buildSQL(Map<String, Argument> arguments, List<AggregatableField> fields) {
        return String.format("AVG(%s)", fields.get(0).getName());
    }
}
