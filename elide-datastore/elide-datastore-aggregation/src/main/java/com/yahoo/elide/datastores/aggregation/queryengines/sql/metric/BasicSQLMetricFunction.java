/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql.metric;

import com.yahoo.elide.datastores.aggregation.metadata.metric.AggregatedField;
import com.yahoo.elide.datastores.aggregation.metadata.metric.MetricFunctionInvocation;
import com.yahoo.elide.datastores.aggregation.metadata.models.FunctionArgument;
import com.yahoo.elide.datastores.aggregation.metadata.models.Metric;
import com.yahoo.elide.datastores.aggregation.query.ColumnProjection;
import com.yahoo.elide.datastores.aggregation.query.TimeDimensionProjection;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.query.SQLQueryTemplate;
import com.yahoo.elide.request.Argument;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A basic implementation of {@link SQLMetricFunction} with attributes and simple resolve methods.
 */
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@Data
public abstract class BasicSQLMetricFunction extends SQLMetricFunction {
    private String name;

    private String longName;

    private String description;

    private Set<FunctionArgument> arguments;

    public BasicSQLMetricFunction(String name, String longName, String description) {
        this(name, longName, description, Collections.emptySet());
    }

    @Override
    protected SQLMetricFunctionInvocation invokeAsSQL(Map<String, Argument> arguments,
                                                      AggregatedField field,
                                                      String alias) {
        final SQLMetricFunction function = this;
        return new SQLMetricFunctionInvocation() {
            @Override
            public SQLMetricFunction getFunction() {
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

    @Override
    public SQLQueryTemplate resolve(Map<String, Argument> arguments,
                                    Metric metric,
                                    String alias,
                                    Set<ColumnProjection> dimensions,
                                    TimeDimensionProjection timeDimension) {
        SQLMetricFunctionInvocation invoked = invokeAsSQL(arguments, new AggregatedField(metric), alias);
        return new SQLQueryTemplate() {
            @Override
            public List<SQLMetricFunctionInvocation> getMetrics() {
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

            @Override
            public boolean isFromTable() {
                return true;
            }

            @Override
            public SQLQueryTemplate getSubQuery() {
                return null;
            }
        };
    }

    @Override
    public SQLQueryTemplate resolve(Map<String, Argument> arguments,
                                    MetricFunctionInvocation metric,
                                    String alias,
                                    SQLQueryTemplate subQuery) {
        SQLMetricFunctionInvocation invoked = invokeAsSQL(arguments, new AggregatedField(metric.getAlias()), alias);
        return new SQLQueryTemplate() {
            @Override
            public List<SQLMetricFunctionInvocation> getMetrics() {
                return Collections.singletonList(invoked);
            }

            @Override
            public Set<ColumnProjection> getNonTimeDimensions() {
                return subQuery.getNonTimeDimensions();
            }

            @Override
            public TimeDimensionProjection getTimeDimension() {
                return subQuery.getTimeDimension();
            }

            @Override
            public boolean isFromTable() {
                return false;
            }

            @Override
            public SQLQueryTemplate getSubQuery() {
                return subQuery;
            }
        };
    }
}
