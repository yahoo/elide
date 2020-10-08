/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql.query;

import com.yahoo.elide.datastores.aggregation.metadata.models.Metric;
import com.yahoo.elide.datastores.aggregation.query.MetricProjection;
import com.yahoo.elide.datastores.aggregation.query.Queryable;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLReferenceTable;
import com.yahoo.elide.request.Argument;

import lombok.Value;

import java.util.Map;

/**
 * Metric projection that can expand the metric into a SQL projection fragment.
 */
@Value
public class SQLMetricProjection implements MetricProjection, SQLColumnProjection<Metric> {

    Metric column;
    SQLReferenceTable referenceTable;
    String alias;
    Map<String, Argument> arguments;

    @Override
    public String toSQL(Queryable query) {
        return referenceTable.getResolvedReference(column.getTable(), column.getName());
    }

    @Override
    public Queryable getSource() {
        return column.getTable();
    }

    @Override
    public String getId() {
        return column.getId();
    }
}
