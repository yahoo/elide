/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql.metric;

import com.yahoo.elide.core.exceptions.InternalServerErrorException;
import com.yahoo.elide.datastores.aggregation.metadata.metric.AggregatableField;
import com.yahoo.elide.datastores.aggregation.metadata.metric.MetricFunctionInvocation;
import com.yahoo.elide.datastores.aggregation.metadata.models.MetricFunction;
import com.yahoo.elide.datastores.aggregation.query.ColumnProjection;
import com.yahoo.elide.datastores.aggregation.query.TimeDimensionProjection;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.query.SQLQueryTemplate;
import com.yahoo.elide.request.Argument;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * SQL extension of {@link MetricFunction} which would be invoked as sql and can construct sql templates.
 */
public abstract class SQLMetricFunction extends MetricFunction {
    /**
     * Construct sql expression with provided arguments and aggregated fields.
     *
     * @param arguments arguments
     * @param fields fields
     * @return <code>FUNCTION(arg1, arg2, arg3, ...,  field1, field2, field3, ...)</code>
     */
    protected String buildSQL(Map<String, Argument> arguments, List<AggregatableField> fields) {
        throw new InternalServerErrorException("Metric function " + getName() + " doesn't have expression.");
    }

    @Override
    public final MetricFunctionInvocation invoke(Map<String, Argument> arguments,
                                                 List<AggregatableField> fields,
                                                 String alias) {
        return invokeAsSQL(arguments, fields, alias);
    }

    /**
     * Invoke this function as sql function.
     *
     * @param arguments arguments provided in the request
     * @param fields fields to apply this function
     * @param alias result alias
     * @return an invoked sql metric function
     */
    protected abstract SQLMetricFunctionInvocation invokeAsSQL(Map<String, Argument> arguments,
                                                               List<AggregatableField> fields,
                                                               String alias);

    /**
     * Construct a sql query template for a physical table with provided information.
     * Table name would be filled in when convert the template into real query.
     *
     * @param arguments arguments provided in the request
     * @param fields metric fields to apply this function
     * @param alias result alias
     * @param dimensions groupBy dimensions
     * @param timeDimension aggregated time dimension
     * @return <code>SELECT function(metric, arguments) AS alias GROUP BY dimensions, timeDimension </code>
     */
    public abstract SQLQueryTemplate resolve(Map<String, Argument> arguments,
                                             List<AggregatableField> fields,
                                             String alias,
                                             Set<ColumnProjection> dimensions,
                                             TimeDimensionProjection timeDimension);

    /**
     * Construct a sql query template for a subquery with provided information.
     * Projections dimension would match the subquery dimensions.
     *
     * @param arguments arguments provided in the request
     * @param fields aggregated metric fields in subquery
     * @param alias result alias
     * @param subQuery subquery temple
     * @return <code>SELECT function(metric, arguments) AS alias FROM subquery</code>
     */
    public abstract SQLQueryTemplate resolve(Map<String, Argument> arguments,
                                             List<AggregatableField> fields,
                                             String alias,
                                             SQLQueryTemplate subQuery);
}
