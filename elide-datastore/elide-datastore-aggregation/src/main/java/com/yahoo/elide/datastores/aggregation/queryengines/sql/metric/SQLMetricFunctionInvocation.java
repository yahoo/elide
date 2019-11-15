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
 * Represents an invoked sql metric function with alias and arguments provided in user request.
 */
public interface SQLMetricFunctionInvocation extends MetricFunctionInvocation {
    @Override
    SQLMetricFunction getFunction();

    /**
     * Get sql expression of this invocation.
     *
     * @return e.g. <code>SUM(field1)</code>
     */
    String getSQL();

    /**
     * Construct a query template for this invocation.
     *
     * @param dimensions groupBy dimensions
     * @param timeDimension time dimension
     * @return a sql query template
     */
    default SQLQueryTemplate resolve(Set<ColumnProjection> dimensions, TimeDimensionProjection timeDimension) {
        return getFunction().resolve(
                getArguments().stream().collect(Collectors.toMap(Argument::getName, Function.identity())),
                getFields(),
                getAlias(),
                dimensions,
                timeDimension);
    }
}
