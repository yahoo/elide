package com.yahoo.elide.datastores.aggregation.metadata.metric;

import com.yahoo.elide.datastores.aggregation.metadata.models.FunctionArgument;
import com.yahoo.elide.datastores.aggregation.metadata.models.MetricFunction;
import com.yahoo.elide.request.Argument;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;
import java.util.Set;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
public class MetricFunctionImpl extends MetricFunction {
    private String name;

    private String longName;

    private String description;

    private Set<FunctionArgument> arguments;

    @Override
    public MetricFunctionInvocation invoke(Map<String, Argument> arguments, AggregatedField field, String alias) {
        final MetricFunction function = this;
        return new MetricFunctionInvocation() {
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
}
