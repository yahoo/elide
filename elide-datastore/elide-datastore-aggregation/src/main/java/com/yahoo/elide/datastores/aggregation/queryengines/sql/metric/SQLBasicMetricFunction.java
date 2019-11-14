/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql.metric;

import com.yahoo.elide.core.exceptions.InternalServerErrorException;
import com.yahoo.elide.core.exceptions.InvalidOperationException;
import com.yahoo.elide.datastores.aggregation.metadata.metric.AggregatedField;
import com.yahoo.elide.datastores.aggregation.metadata.metric.BasicMetricFunction;
import com.yahoo.elide.datastores.aggregation.metadata.metric.MetricFunctionInvocation;
import com.yahoo.elide.datastores.aggregation.metadata.models.FunctionArgument;
import com.yahoo.elide.datastores.aggregation.metadata.models.Metric;
import com.yahoo.elide.datastores.aggregation.query.DimensionProjection;
import com.yahoo.elide.datastores.aggregation.query.TimeDimensionProjection;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.query.SQLQueryTemplate;
import com.yahoo.elide.request.Argument;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * SQL extension of {@link BasicMetricFunction} which would be invoked as sql and can construct sql templates.
 */
public abstract class SQLBasicMetricFunction extends BasicMetricFunction {
    public SQLBasicMetricFunction(String name, String longName, String description, Set<FunctionArgument> arguments) {
        super(name, longName, description, arguments);
    }

    /**
     * Get SQL expression string of this metric.
     *
     * @return e.g. <code>SUM(%metric)</code>
     */
    public String getSQLExpression() {
        throw new InternalServerErrorException("Metric function " + getName() + " doesn't have expression.");
    }

    @Override
    public final MetricFunctionInvocation invoke(Map<String, Argument> arguments, AggregatedField field, String alias) {
        return invokeAsSQL(arguments, field, alias);
    }

    /**
     * Invoke this function as sql function.
     *
     * @param arguments arguments provided in the request
     * @param field field to apply this function
     * @param alias result alias
     * @return an invoked sql metric function
     */
    protected SQLMetricFunctionInvocation invokeAsSQL(Map<String, Argument> arguments,
                                                      AggregatedField field,
                                                      String alias) {
        final SQLBasicMetricFunction function = this;
        return new SQLMetricFunctionInvocation() {
            @Override
            public SQLBasicMetricFunction getFunction() {
                return function;
            }

            @Override
            public List<Argument> getArguments() {
                return new ArrayList<>(arguments.values());
            }

            @Override
            public Argument getArgument(String argumentName) {
                return arguments.get(argumentName);
            }

            @Override
            public AggregatedField getAggregatedField() {
                return field;
            }

            @Override
            public String getAlias() {
                return alias;
            }
        };
    }

    /**
     * Construct a sql query template for a physical table with provided information.
     * Table name would be filled in when convert the template into real query.
     *
     * @param arguments arguments provided in the request
     * @param metric field to apply this function
     * @param alias result alias
     * @param dimensions groupBy dimensions
     * @param timeDimension aggregated time dimension
     * @return <code>SELECT function(metric, arguments) AS alias GROUP BY dimensions, timeDimension </code>
     */
    public abstract SQLQueryTemplate resolve(Map<String, Argument> arguments,
                                             Metric metric,
                                             String alias,
                                             Set<DimensionProjection> dimensions,
                                             TimeDimensionProjection timeDimension);

    /**
     * Construct a sql query template for a subquery with provided information.
     * Projections dimension would match the subquery dimensions.
     *
     * @param arguments arguments provided in the request
     * @param metric aggregated metric field in subquery
     * @param alias result alias
     * @param subQuery subquery temple
     * @return <code>SELECT function(metric, arguments) AS alias FROM subquery</code>
     */
    public SQLQueryTemplate resolve(Map<String, Argument> arguments,
                                    MetricFunctionInvocation metric,
                                    String alias,
                                    SQLQueryTemplate subQuery) {
        throw new InvalidOperationException("Can't apply aggregation " + getName() + " on nested query.");
    }
}
