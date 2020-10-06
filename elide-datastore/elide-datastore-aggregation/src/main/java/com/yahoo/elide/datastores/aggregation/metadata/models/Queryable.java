/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.metadata.models;

import java.util.Set;

public interface Queryable {
    public String getName();
    public String getAlias();
    public String getVersion();
    public Dimension getDimension(String name);
    public Set<Dimension> getDimensions();
    public Metric getMetric(String name);
    public Set<Metric> getMetrics();
    public TimeDimension getTimeDimension(String name);
    public Set<TimeDimension> getTimeDimensions();
    public Set<Column> getColumns();
}
