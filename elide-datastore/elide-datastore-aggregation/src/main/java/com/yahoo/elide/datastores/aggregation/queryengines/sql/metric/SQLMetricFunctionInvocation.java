/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql.metric;

import com.yahoo.elide.datastores.aggregation.metadata.metric.MetricFunctionInvocation;
import com.yahoo.elide.datastores.aggregation.query.DimensionProjection;
import com.yahoo.elide.datastores.aggregation.query.TimeDimensionProjection;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.templates.SQLQueryTemplate;

import java.util.Set;

/**
 * Represents an invoked metric function with alias and arguments provided in user request.
 */
public interface SQLMetricFunctionInvocation extends MetricFunctionInvocation {
    SQLMetricFunction getSQLMetricFunction();

    // TODO: insert arguments into expression
    default String toSQLExpression() {
        return getSQLMetricFunction().getExpression().replace("%metric", getAggregatedField().getFieldName());
    }

    default SQLQueryTemplate resolve(Set<DimensionProjection> dimensions, TimeDimensionProjection timeDimension) {
        return getSQLMetricFunction().resolve(
                getArguments(),
                getAggregatedField().getMetric(),
                getAlias(),
                dimensions,
                timeDimension);
    }
}
