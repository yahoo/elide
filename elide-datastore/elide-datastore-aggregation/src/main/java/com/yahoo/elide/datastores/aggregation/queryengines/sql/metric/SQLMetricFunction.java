package com.yahoo.elide.datastores.aggregation.queryengines.sql.metric;

import com.yahoo.elide.core.exceptions.InternalServerErrorException;
import com.yahoo.elide.core.exceptions.InvalidOperationException;
import com.yahoo.elide.datastores.aggregation.metadata.metric.AggregatedField;
import com.yahoo.elide.datastores.aggregation.metadata.metric.MetricFunctionImpl;
import com.yahoo.elide.datastores.aggregation.metadata.metric.MetricFunctionInvocation;
import com.yahoo.elide.datastores.aggregation.metadata.models.FunctionArgument;
import com.yahoo.elide.datastores.aggregation.metadata.models.Metric;
import com.yahoo.elide.datastores.aggregation.metadata.models.MetricFunction;
import com.yahoo.elide.datastores.aggregation.query.DimensionProjection;
import com.yahoo.elide.datastores.aggregation.query.TimeDimensionProjection;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.templates.SQLQueryTemplate;
import com.yahoo.elide.request.Argument;

import java.util.Map;
import java.util.Set;

public abstract class SQLMetricFunction extends MetricFunctionImpl {
    public SQLMetricFunction(String name, String longName, String description, Set<FunctionArgument> arguments) {
        super(name, longName, description, arguments);
    }

    @Override
    public final MetricFunctionInvocation invoke(Map<String, Argument> arguments, AggregatedField field, String alias) {
        return invokeAsSQL(arguments, field, alias);
    }

    protected SQLMetricFunctionInvocation invokeAsSQL(Map<String, Argument> arguments,
                                                      AggregatedField field,
                                                      String alias) {
        final SQLMetricFunction function = this;
        return new SQLMetricFunctionInvocation() {
            @Override
            public SQLMetricFunction getSQLMetricFunction() {
                return function;
            }

            @Override
            public Map<String, Argument> getArguments() {
                return arguments;
            }

            @Override
            public MetricFunction getFunction() {
                return function;
            }

            @Override
            public AggregatedField getAggregatedField() {
                return field;
            }

            @Override
            public String getAlias() {
                return alias;
            }
        };
    }

    public abstract SQLQueryTemplate resolve(Map<String, Argument> arguments,
                                             Metric metric,
                                             String alias,
                                             Set<DimensionProjection> dimensions,
                                             TimeDimensionProjection timeDimension);

    public SQLQueryTemplate resolve(Map<String, Argument> arguments,
                                    MetricFunctionInvocation metric,
                                    String alias,
                                    SQLQueryTemplate subQuery) {
        throw new InvalidOperationException("Can't apply aggregation " + getName() + " on nested query.");
    }

    public String getExpression() {
        throw new InternalServerErrorException("Metric function " + getName() + " doesn't have expression.");
    }
}
