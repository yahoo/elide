package com.yahoo.elide.datastores.aggregation.queryengines.sql.query.plan;

import com.yahoo.elide.datastores.aggregation.metadata.models.Dimension;
import com.yahoo.elide.datastores.aggregation.metadata.models.Metric;
import com.yahoo.elide.datastores.aggregation.metadata.models.TimeDimension;

public interface Source {
    public String getAlias();
    public <T> T accept(QueryPlanVisitor<T> visitor);

    public Dimension getDimension(String name);
    public Metric getMetric(String name);
    public TimeDimension getTimeDimension(String name);
}
