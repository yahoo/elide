/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql.metric;

import com.yahoo.elide.datastores.aggregation.metadata.models.metric.MetricFunctionInvocation;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.templates.SQLQueryTemplate;

/**
 * Represents an invoked metric function with alias and arguments provided in user request.
 */
public interface SQLMetricFunctionInvocation extends MetricFunctionInvocation {
    SQLQueryTemplate getQuery();
}
