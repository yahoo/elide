package com.yahoo.elide.datastores.aggregation.queryengines.sql.query.plan;

import com.yahoo.elide.datastores.aggregation.metadata.models.Dimension;
import com.yahoo.elide.datastores.aggregation.metadata.models.Metric;
import com.yahoo.elide.datastores.aggregation.metadata.models.TimeDimension;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLTable;
import com.yahoo.elide.utils.TypeHelper;
import lombok.Value;

@Value
public class TableSource implements Source {
    private SQLTable table;

    public String getAlias() {
        return TypeHelper.getTypeAlias(table.getClass());
    }

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
}
