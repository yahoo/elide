/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql.metric.functions;

import com.yahoo.elide.datastores.aggregation.queryengines.sql.metric.SQLMetricFunction;

import java.util.Collections;

/**
 * Max of a field.
 */
public class SqlMax extends SQLMetricFunction {
    public SqlMax() {
        super("max", "sql max function", "MAX(%s)", Collections.emptySet());
    }
}
