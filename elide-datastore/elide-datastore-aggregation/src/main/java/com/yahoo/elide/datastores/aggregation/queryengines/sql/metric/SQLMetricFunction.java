package com.yahoo.elide.datastores.aggregation.queryengines.sql.metric;

import com.yahoo.elide.core.exceptions.InvalidOperationException;
import com.yahoo.elide.datastores.aggregation.metadata.models.metric.AggregatedField;
import com.yahoo.elide.datastores.aggregation.metadata.models.metric.MetricFunctionImpl;
import com.yahoo.elide.datastores.aggregation.metadata.models.metric.MetricFunctionInvocation;
import com.yahoo.elide.datastores.aggregation.metadata.models.Dimension;
import com.yahoo.elide.datastores.aggregation.metadata.models.FunctionArgument;
import com.yahoo.elide.datastores.aggregation.metadata.models.Metric;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.templates.SQLQueryTemplate;
import com.yahoo.elide.datastores.aggregation.time.RequestTimeDimension;
import com.yahoo.elide.request.Argument;

import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class SQLMetricFunction extends MetricFunctionImpl {
    public SQLMetricFunction(String name, String longName, String description, Set<FunctionArgument> arguments) {
        super(name, longName, description, arguments);
    }

    @Override
    public MetricFunctionInvocation invoke(Map<String, Argument> arguments, AggregatedField field, String alias) {
        return super.invoke(arguments, field, alias);
    }

    public abstract SQLQueryTemplate resolve(Map<String, Argument> arguments,
                                             Metric metric,
                                             String alias,
                                             List<Dimension> dimensions,
                                             RequestTimeDimension timeDimension);

    public SQLQueryTemplate resolve(Map<String, Argument> arguments,
                                    MetricFunctionInvocation metric,
                                    String alias,
                                    SQLQueryTemplate subQuery) {
        throw new InvalidOperationException("Can't apply aggregation " + getName() + " on nested query.");
    }
}
