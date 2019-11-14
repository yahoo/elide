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
import com.yahoo.elide.datastores.aggregation.query.DimensionProjection;
import com.yahoo.elide.datastores.aggregation.query.TimeDimensionProjection;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.query.SQLQueryTemplate;
import com.yahoo.elide.request.Argument;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Sql Metric Functions that can be applied on a physical metric field or an aggregated metric field.
 */
public abstract class SimpleSQLMetricFunction extends SQLBasicMetricFunction {
    public SimpleSQLMetricFunction(String name, String longName, String description, Set<FunctionArgument> arguments) {
        super(name, longName, description, arguments);
    }

    public SimpleSQLMetricFunction(String name, String longName, String description) {
        super(name, longName, description, Collections.emptySet());
    }

    @Override
    public SQLQueryTemplate resolve(Map<String, Argument> arguments,
                                    Metric metric,
                                    String alias,
                                    Set<DimensionProjection> dimensions,
                                    TimeDimensionProjection timeDimension) {
        SQLMetricFunctionInvocation invoked = invokeAsSQL(arguments, new AggregatedField(metric), alias);
        return new SQLQueryTemplate() {
            @Override
            public List<SQLMetricFunctionInvocation> getMetrics() {
                return Collections.singletonList(invoked);
            }

            @Override
            public Set<DimensionProjection> getNonTimeDimensions() {
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
            public Set<DimensionProjection> getNonTimeDimensions() {
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
