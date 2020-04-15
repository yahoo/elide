/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.datastores.aggregation.metadata.models.FunctionArgument;
import com.yahoo.elide.datastores.aggregation.metadata.models.Metric;
import com.yahoo.elide.datastores.aggregation.metadata.models.MetricFunction;
import com.yahoo.elide.datastores.aggregation.query.ColumnProjection;
import com.yahoo.elide.datastores.aggregation.query.MetricProjection;
import com.yahoo.elide.datastores.aggregation.query.TimeDimensionProjection;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metric.SQLMetricFunction;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.query.SQLMetricProjection;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.query.SQLQueryTemplate;
import com.yahoo.elide.request.Argument;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * SQLMetric would contain {@link SQLMetricFunction} instead of {@link MetricFunction}.
 */
public class SQLMetric extends Metric {
    public SQLMetric(SQLTable table, String fieldName, EntityDictionary dictionary) {
        super(table, fieldName, dictionary);
    }

    @Override
    protected SQLMetricFunction constructMetricFunction(String id,
                                                     String longName,
                                                     String description,
                                                     String expression,
                                                     Set<FunctionArgument> arguments) {
        return new SQLMetricFunction(id, longName, description, expression, arguments);
    }

    /**
     * Construct a sql query template for a physical table with provided information.
     * Table name would be filled in when convert the template into real query.
     *
     * @param sqlReferenceTable Table which expends SQL fragments
     * @param arguments arguments provided in the request
     * @param alias result alias
     * @param dimensions groupBy dimensions
     * @param timeDimension aggregated time dimension
     * @return <code>SELECT function(arguments, fields) AS alias GROUP BY dimensions, timeDimension </code>
     */
    public SQLQueryTemplate resolve(SQLReferenceTable sqlReferenceTable,
                                    Map<String, Argument> arguments,
                                    String alias,
                                    Set<ColumnProjection> dimensions,
                                    TimeDimensionProjection timeDimension) {
        MetricProjection projection = ColumnProjection.toMetricProjection(this, alias, arguments);

        return new SQLQueryTemplate() {
            @Override
            public SQLTable getTable() {
                return (SQLTable) projection.getColumn().getTable();
            }

            @Override
            public List<SQLMetricProjection> getMetrics() {
                SQLMetricProjection sqlProjection = new SQLMetricProjection(SQLMetric.this,
                        sqlReferenceTable, alias, arguments);

                return Collections.singletonList(sqlProjection);
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
