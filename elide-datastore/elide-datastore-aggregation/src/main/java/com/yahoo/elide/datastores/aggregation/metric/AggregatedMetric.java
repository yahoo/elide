/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metric;

import com.yahoo.elide.datastores.aggregation.Column;
import com.yahoo.elide.datastores.aggregation.Schema;
import com.yahoo.elide.datastores.aggregation.annotation.Meta;
import com.yahoo.elide.datastores.aggregation.annotation.MetricAggregation;

import com.yahoo.elide.datastores.aggregation.schema.Schema;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.stream.Collectors;

/**
 * {@link Metric} annotated by {@link MetricAggregation}.
 * <p>
 * {@link AggregatedMetric} is thread-safe and can be accessed by multiple threads.
 */
@Slf4j
public class AggregatedMetric extends Column implements Metric {

    private static final long serialVersionUID = 3055820948480283917L;

    @Getter
    private final List<Class<? extends Aggregation>> aggregations;

    /**
     * Constructor.
     *
     * @param schema The schema this {@link Metric} belongs to
     * @param metricField  The entity field or relation that this {@link Metric} represents
     * @param annotation  Provides static meta data about this {@link Metric}
     * @param fieldType  The Java type for this entity field or relation
     * @param aggregations  A list of all supported aggregations on this {@link Metric}
     */
    public AggregatedMetric(
            Schema schema,
            String metricField,
            Meta annotation,
            Class<?> fieldType,
            List<Class<? extends Aggregation>> aggregations
    ) {
        super(schema, metricField, annotation, fieldType);

        // make sure we have at least 1 aggregation so we can generate static SQL functions
        if (aggregations.isEmpty()) {
            String message = String.format("'%s' has no aggregation.", metricField);
            log.error(message);
            throw new IllegalStateException(message);
        }

        this.aggregations = aggregations;
    }

    @Override
    public String getMetricExpression(final Optional<Class<? extends Aggregation>> aggregation) {
        if (!aggregation.isPresent()) {
            return "";
        }

        try {
            Class<?> clazz = Class.forName(aggregation.get().getCanonicalName());
            Constructor<?> ctor = clazz.getConstructor();
            Aggregation instance = (Aggregation) ctor.newInstance();
            return String.format(instance.getAggFunctionFormat(), schema.getAlias() + "." + name);
        } catch (Exception exception) {
            String message = String.format(
                    "Cannot generate aggregation function for '%s'",
                    aggregation.get().getCanonicalName()
            );
            log.error(message, exception);
            throw new IllegalStateException(message, exception);
        }
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false; }
        if (!super.equals(other)) {
            return false;
        }

        final AggregatedMetric that = (AggregatedMetric) other;
        return getAggregations().equals(that.getAggregations());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getAggregations());
    }

    /**
     * Returns the string representation of this {@link Metric}.
     * <p>
     * The string consists of values of all fields in the format
     * "AggregatedMetric[name='XXX', longName='XXX', description='XXX', dataType=XXX, defaultAggregation=XXX,
     * aggregations=XXX, YYY, ..., metricExpression='XXX']", where values can be programmatically fetched via
     * getters.
     * <p>
     * Aggregation classes are printed in its {@link Class#getSimpleName() simple name} and {@link Optional#empty()} as
     * 'N/A'
     * <p>
     * Note that there is a single space separating each value pair.
     *
     * @return serialized {@link AggregatedMetric}
     */
    @Override
    public String toString() {
        return new StringJoiner(", ", AggregatedMetric.class.getSimpleName() + "[", "]")
                .add("name='" + getName() + "'")
                .add("longName='" + getLongName() + "'")
                .add("description='" + getDescription() + "'")
                .add("dataType=" + getDataType())
                .add("aggregations="
                        + getAggregations().stream()
                                .map(Class::getSimpleName)
                                .collect(Collectors.joining(", "))
                )
                .toString();
    }
}
