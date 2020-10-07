/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.query;

import com.yahoo.elide.datastores.aggregation.metadata.models.Column;
import com.yahoo.elide.datastores.aggregation.metadata.models.Dimension;
import com.yahoo.elide.datastores.aggregation.metadata.models.Metric;
import com.yahoo.elide.datastores.aggregation.metadata.models.TimeDimension;

import java.util.Set;

public interface Queryable {
    public String getAlias();
    public Dimension getDimension(String name);
    public Set<Dimension> getDimensions();
    public Metric getMetric(String name);
    public Set<Metric> getMetrics();
    public TimeDimension getTimeDimension(String name);
    public Set<TimeDimension> getTimeDimensions();
    public Set<Column> getColumns();
    public String getDbConnectionName();
    public <T> T accept(QueryVisitor<T> visitor);

    default public boolean isStatic() {
        return true;
    }
}
