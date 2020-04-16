/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.query;

import com.yahoo.elide.core.exceptions.InvalidOperationException;
import com.yahoo.elide.core.exceptions.InvalidPredicateException;
import com.yahoo.elide.datastores.aggregation.metadata.enums.TimeGrain;
import com.yahoo.elide.datastores.aggregation.metadata.models.Column;
import com.yahoo.elide.datastores.aggregation.metadata.models.Dimension;
import com.yahoo.elide.datastores.aggregation.metadata.models.FunctionArgument;
import com.yahoo.elide.datastores.aggregation.metadata.models.Metric;
import com.yahoo.elide.datastores.aggregation.metadata.models.MetricFunction;
import com.yahoo.elide.datastores.aggregation.metadata.models.TimeDimension;
import com.yahoo.elide.datastores.aggregation.metadata.models.TimeDimensionGrain;
import com.yahoo.elide.request.Argument;

import java.io.Serializable;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Collectors;

/**
 * Represents a projected column as an alias in a query.
 * @param <T> Column type of the projection.
 */
public interface ColumnProjection<T extends Column> extends Serializable {
    /**
     * Get the projected column.
     *
     * @return column
     */
    T getColumn();

    /**
     * Get the projection alias.
     *
     * @return alias
     */
    String getAlias();

    /**
     * Get all arguments provided for this metric function.
     *
     * @return request arguments
     */
    Map<String, Argument> getArguments();

    /**
     * Project a dimension as alias.
     *
     * @param dimension dimension column
     * @param alias alias
     * @param arguments arguments of this projection
     * @return a projection represents that "dimension AS alias"
     */
    static ColumnProjection toDimensionProjection(Dimension dimension, String alias, Map<String, Argument> arguments) {
        return new ColumnProjection() {
            @Override
            public Column getColumn() {
                return dimension;
            }

            @Override
            public String getAlias() {
                return alias;
            }

            @Override
            public Map<String, Argument> getArguments() {
                return arguments;
            }
        };
    }

    /**
     * Project a time dimension as alias with specific time grain.
     *
     * @param dimension time dimension column
     * @param alias alias
     * @param arguments arguments of this projection
     * @return a projection represents that "grain(dimension) AS alias"
     */
    static TimeDimensionProjection toTimeDimensionProjection(TimeDimension dimension,
                                                             String alias,
                                                             Map<String, Argument> arguments) {
        Argument grainArgument = arguments.get("grain");

        TimeDimensionGrain resolvedGrain;
        if (grainArgument == null) {
            //The first grain is the default.
            resolvedGrain = dimension.getSupportedGrains().stream()
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            String.format("Requested default grain, no grain defined on %s",
                                    dimension.getName())));
        } else {
            String requestedGrainName = grainArgument.getValue().toString().toLowerCase(Locale.ENGLISH);

            resolvedGrain = dimension.getSupportedGrains().stream()
                    .filter(supportedGrain -> supportedGrain.getGrain().name().toLowerCase(Locale.ENGLISH)
                            .equals(requestedGrainName))
                    .findFirst()
                    .orElseThrow(() -> new InvalidOperationException(
                            String.format("Unsupported grain %s for field %s",
                                    requestedGrainName,
                                    dimension.getName())));
        }

        return new TimeDimensionProjection() {
            @Override
            public TimeDimension getColumn() {
                return dimension;
            }

            @Override
            public String getAlias() {
                return alias;
            }

            @Override
            public Map<String, Argument> getArguments() {
                return arguments;
            }

            @Override
            public TimeGrain getGrain() {
                return resolvedGrain.getGrain();
            }

            @Override
            public TimeZone getTimeZone() {
                return null;
            }
        };
    }

    static MetricProjection toMetricProjection(Metric metric, String alias, Map<String, Argument> arguments) {
        MetricFunction function = metric.getMetricFunction();

        Set<String> requiredArguments = function.getArguments().stream()
                .map(FunctionArgument::getName)
                .collect(Collectors.toSet());

        if (!requiredArguments.equals(arguments.keySet())) {
            throw new InvalidPredicateException(
                    "Provided arguments doesn't match requirement for function " + function.getName() + ".");
        }

        return new MetricProjection() {
            @Override
            public Metric getColumn() {
                return metric;
            }

            @Override
            public String getAlias() {
                return alias;
            }

            @Override
            public Map<String, Argument> getArguments() {
                return arguments;
            }
        };
    }
}
