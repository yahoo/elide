package com.yahoo.elide.datastores.aggregation.queryengines.sql.templates;

import com.yahoo.elide.datastores.aggregation.metadata.models.metric.MetricFunctionInvocation;
import com.yahoo.elide.datastores.aggregation.metadata.models.Dimension;
import com.yahoo.elide.datastores.aggregation.time.RequestTimeDimension;
import com.yahoo.elide.datastores.aggregation.time.TimeGrain;

import java.util.List;

public interface SQLQueryTemplate {
    List<MetricFunctionInvocation> getMetrics();
    List<Dimension> getGroupByDimensions();
    RequestTimeDimension getTimeDimension();
    boolean isFromTable();
    SQLQueryTemplate getSubQuery();

    default int getLevel() {
        return isFromTable() ? 1 : 1 + getSubQuery().getLevel();
    }

    default SQLQueryTemplate withTimeGrain(TimeGrain timeGrain) {
        SQLQueryTemplate wrapped = this;
        return new SQLQueryTemplate() {
            @Override
            public List<MetricFunctionInvocation> getMetrics() {
                return wrapped.getMetrics();
            }

            @Override
            public List<Dimension> getGroupByDimensions() {
                return wrapped.getGroupByDimensions();
            }

            @Override
            public RequestTimeDimension getTimeDimension() {
                return new RequestTimeDimension(wrapped.getTimeDimension().getTimeDimension(), timeGrain);
            }

            @Override
            public boolean isFromTable() {
                return wrapped.isFromTable();
            }

            @Override
            public SQLQueryTemplate getSubQuery() {
                return wrapped.getSubQuery();
            }
        };
    }
}
