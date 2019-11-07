package com.yahoo.elide.datastores.aggregation.queryengines.sql.metric;

import com.yahoo.elide.datastores.aggregation.metadata.models.metric.AggregatedField;
import com.yahoo.elide.datastores.aggregation.metadata.models.metric.MetricFunctionInvocation;
import com.yahoo.elide.datastores.aggregation.metadata.models.Dimension;
import com.yahoo.elide.datastores.aggregation.metadata.models.FunctionArgument;
import com.yahoo.elide.datastores.aggregation.metadata.models.Metric;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.templates.SQLQueryTemplate;
import com.yahoo.elide.datastores.aggregation.time.RequestTimeDimension;
import com.yahoo.elide.request.Argument;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class SimpleSQLMetricFunction extends SQLMetricFunction {
    public SimpleSQLMetricFunction(String name, String longName, String description, Set<FunctionArgument> arguments) {
        super(name, longName, description, arguments);
    }

    public SimpleSQLMetricFunction(String name, String longName, String description) {
        super(name, longName, description, Collections.emptySet());
    }

    @Override
    public SQLQueryTemplate resolve(Map<String, Argument> arguments,
                                    Metric metric,
                                    String alias,
                                    List<Dimension> dimensions,
                                    RequestTimeDimension timeDimension) {
        MetricFunctionInvocation invoked = invoke(arguments, new AggregatedField(metric), alias);
        return new SQLQueryTemplate() {
            @Override
            public List<MetricFunctionInvocation> getMetrics() {
                return Collections.singletonList(invoked);
            }

            @Override
            public List<Dimension> getGroupByDimensions() {
                return dimensions;
            }

            @Override
            public RequestTimeDimension getTimeDimension() {
                return timeDimension;
            }

            @Override
            public boolean isFromTable() {
                return true;
            }

            @Override
            public SQLQueryTemplate getSubQuery() {
                return null;
            }
        };
    }

    @Override
    public SQLQueryTemplate resolve(Map<String, Argument> arguments,
                                    MetricFunctionInvocation metric,
                                    String alias,
                                    SQLQueryTemplate subQuery) {
        MetricFunctionInvocation invoked = invoke(arguments, new AggregatedField(metric.getAlias()), alias);
        return new SQLQueryTemplate() {
            @Override
            public List<MetricFunctionInvocation> getMetrics() {
                return Collections.singletonList(invoked);
            }

            @Override
            public List<Dimension> getGroupByDimensions() {
                return subQuery.getGroupByDimensions();
            }

            @Override
            public RequestTimeDimension getTimeDimension() {
                return subQuery.getTimeDimension();
            }

            @Override
            public boolean isFromTable() {
                return false;
            }

            @Override
            public SQLQueryTemplate getSubQuery() {
                return subQuery;
            }
        };
    }
}
