/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql.metric;

import com.yahoo.elide.datastores.aggregation.metadata.metric.MetricFunctionInvocation;
import com.yahoo.elide.datastores.aggregation.query.ColumnProjection;
import com.yahoo.elide.datastores.aggregation.query.TimeDimensionProjection;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.query.SQLQueryTemplate;
import com.yahoo.elide.request.Argument;

import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Represents an invoked metric function with alias and arguments provided in user request.
 */
public interface SQLMetricFunctionInvocation extends MetricFunctionInvocation {
    SQLMetricFunction getFunction();

    /**
     * Get sql expression of this invocation.
     *
     * @return <code>function(aggregatedField, arguments)</code>
     */
    String getSQL();

    /**
     * Construct a query template based on this invocation.
     *
     * @param dimensions groupBy dimensions
     * @param timeDimension time dimension
     * @return a sql query template
     */
    default SQLQueryTemplate resolve(Set<ColumnProjection> dimensions, TimeDimensionProjection timeDimension) {
        return getFunction().resolve(
                getArguments().stream().collect(Collectors.toMap(Argument::getName, Function.identity())),
                getAggregatables(),
                getAlias(),
                dimensions,
                timeDimension);
    }
}
