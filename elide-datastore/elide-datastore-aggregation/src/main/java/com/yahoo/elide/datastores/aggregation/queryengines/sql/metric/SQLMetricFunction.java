/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql.metric;

import com.yahoo.elide.datastores.aggregation.metadata.metric.MetricFunctionInvocation;
import com.yahoo.elide.datastores.aggregation.metadata.models.FunctionArgument;
import com.yahoo.elide.datastores.aggregation.metadata.models.MetricFunction;
import com.yahoo.elide.datastores.aggregation.query.ColumnProjection;
import com.yahoo.elide.datastores.aggregation.query.TimeDimensionProjection;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.query.SQLQueryTemplate;
import com.yahoo.elide.request.Argument;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * SQL extension of {@link MetricFunction} which would be invoked as sql and can construct sql templates.
 */
public class SQLMetricFunction extends MetricFunction {
    public SQLMetricFunction(String name, String longName, String description, String expression,
                             Set<FunctionArgument> arguments) {
        super(name, longName, description, expression, arguments);
    }

    /**
     * Construct a sql query template for a physical table with provided information.
     * Table name would be filled in when convert the template into real query.
     *
     * @param arguments arguments provided in the request
     * @param alias result alias
     * @param dimensions groupBy dimensions
     * @param timeDimension aggregated time dimension
     * @return <code>SELECT function(arguments, fields) AS alias GROUP BY dimensions, timeDimension </code>
     */
    public SQLQueryTemplate resolve(Map<String, Argument> arguments,
                                    String alias,
                                    Set<ColumnProjection> dimensions,
                                    TimeDimensionProjection timeDimension) {
        MetricFunctionInvocation invoked = invoke(arguments, alias);
        return new SQLQueryTemplate() {
            @Override
            public List<MetricFunctionInvocation> getMetrics() {
                return Collections.singletonList(invoked);
            }

            @Override
            public Set<ColumnProjection> getNonTimeDimensions() {
                return dimensions;
            }

            @Override
            public TimeDimensionProjection getTimeDimension() {
                return timeDimension;
            }
        };
    }
}
