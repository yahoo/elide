package com.yahoo.elide.datastores.aggregation.queryengines.sql.templates;

import com.yahoo.elide.core.exceptions.InvalidPredicateException;
import com.yahoo.elide.datastores.aggregation.metadata.metric.MetricFunctionInvocation;
import com.yahoo.elide.datastores.aggregation.query.DimensionProjection;
import com.yahoo.elide.datastores.aggregation.query.TimeDimensionProjection;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metric.SQLMetricFunctionInvocation;
import com.yahoo.elide.datastores.aggregation.time.TimeGrain;

import org.apache.commons.lang3.NotImplementedException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public interface SQLQueryTemplate {
    List<SQLMetricFunctionInvocation> getMetrics();
    Set<DimensionProjection> getGroupByDimensions();
    TimeDimensionProjection getTimeDimension();
    boolean isFromTable();
    SQLQueryTemplate getSubQuery();

    default int getLevel() {
        return isFromTable() ? 1 : 1 + getSubQuery().getLevel();
    }

    default SQLQueryTemplate toTimeGrain(TimeGrain timeGrain) {
        SQLQueryTemplate wrapped = this;
        return new SQLQueryTemplate() {
            @Override
            public List<SQLMetricFunctionInvocation> getMetrics() {
                return wrapped.getMetrics();
            }

            @Override
            public Set<DimensionProjection> getGroupByDimensions() {
                return wrapped.getGroupByDimensions();
            }

            @Override
            public TimeDimensionProjection getTimeDimension() {
                return wrapped.getTimeDimension().toTimeGrain(timeGrain);
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

    default SQLQueryTemplate merge(SQLQueryTemplate second) {
        if (getLevel() != second.getLevel()) {
            throw new InvalidPredicateException("Can't merge two query with different level");
        } else {
            if (getLevel() > 1) {
                throw new NotImplementedException("Merging sql query is not supported.");
            }

            SQLQueryTemplate first = this;
            // TODO: validate dimension
            List<SQLMetricFunctionInvocation> merged = new ArrayList<>(first.getMetrics());
            merged.addAll(second.getMetrics());

            return new SQLQueryTemplate() {
                @Override
                public List<SQLMetricFunctionInvocation> getMetrics() {
                    return merged;
                }

                @Override
                public Set<DimensionProjection> getGroupByDimensions() {
                    return first.getGroupByDimensions();
                }

                @Override
                public TimeDimensionProjection getTimeDimension() {
                    return first.getTimeDimension();
                }

                @Override
                public boolean isFromTable() {
                    return first.isFromTable();
                }

                @Override
                public SQLQueryTemplate getSubQuery() {
                    return first.getSubQuery();
                }
            };
        }
    }
}
