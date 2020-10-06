/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql.query.plan;

import com.yahoo.elide.datastores.aggregation.metadata.models.Column;
import com.yahoo.elide.datastores.aggregation.metadata.models.Dimension;
import com.yahoo.elide.datastores.aggregation.metadata.models.Metric;
import com.yahoo.elide.datastores.aggregation.metadata.models.TimeDimension;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLTable;
import lombok.Value;

import java.util.Set;

@Value
public class TableSource implements Source {
    private SQLTable table;

    @Override
    public String getAlias() {
        return table.getAlias();
    }

    @Override
    public String getName() {
        return table.getName();
    }

    @Override
    public String getVersion() {
        return table.getVersion();
    }

    @Override
    public <T> T accept(QueryPlanVisitor<T> visitor) {
        return visitor.visitTableSource(this);
    }

    @Override
    public Dimension getDimension(String name) {
        return table.getDimension(name);
    }

    @Override
    public Metric getMetric(String name) {
        return table.getMetric(name);
    }

    @Override
    public TimeDimension getTimeDimension(String name) {
        return table.getTimeDimension(name);
    }

    @Override
    public Set<Dimension> getDimensions() {
        return table.getDimensions();
    }

    @Override
    public Set<Metric> getMetrics() {
        return table.getMetrics();
    }

    @Override
    public Set<TimeDimension> getTimeDimensions() {
        return table.getTimeDimensions();
    }

    @Override
    public Set<Column> getColumns() {
        return table.getColumns();
    }
}
