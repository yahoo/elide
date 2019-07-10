/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metric;

import com.yahoo.elide.datastores.aggregation.Column;
import com.yahoo.elide.datastores.aggregation.annotation.Meta;
import com.yahoo.elide.datastores.aggregation.annotation.MetricAggregation;

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
 * {@link BaseMetric} is thread-safe and can be accessed by multiple threads.
 */
@Slf4j
public class BaseMetric extends Column implements Metric {

    /**
     * Given a {@link Aggregation}, returns the SQL aggregation function.
     * <p>
     * For example, returns "MAX(%s)" if the specified {@link Aggregation} is {@link Max}. If the specified aggregation
     * is {@code null}, this method returns an empty string, i.e. "".
     *
     * @param aggregationType  A sub-type of {@link Aggregation}, such as {@link Max} and {@link Min}.
     *
     * @return the SQL function format of the {@code aggregationType}
     */
    public static String functionFormat(Class<? extends Aggregation> aggregationType) {
        if (aggregationType == null) {
            return "";
        }

        try {
            Class<?> clazz = Class.forName(aggregationType.getCanonicalName());
            Constructor<?> ctor = clazz.getConstructor();
            Aggregation instance = (Aggregation) ctor.newInstance();
            return instance.getAggFunctionFormat();
        } catch (Exception exception) {
            String message = String.format(
                    "Cannot generate aggregation function for '%s'",
                    aggregationType.getCanonicalName()
            );
            log.error(message, exception);
            throw new IllegalStateException(message, exception);
        }
    }

    private static final long serialVersionUID = 3055820948480283917L;

    @Getter
    private final List<Class<? extends Aggregation>> aggregations;

    @Getter
    private final String metricExpression;

    /**
     * Constructor.
     *
     * @param metricField  The entity field or relation that this {@link Metric} represents
     * @param annotation  Provides static meta data about this {@link Metric}
     * @param fieldType  The Java type for this entity field or relation
     * @param aggregations  A list of all supported aggregations on this {@link Metric}
     * @param metricExpression  The SQL functions corresponding the default aggregation of this {@link Metric}. Note
     * that the default aggregation is the first aggregation in this list
     */
    public BaseMetric(
            String metricField,
            Meta annotation,
            Class<?> fieldType,
            List<Class<? extends Aggregation>> aggregations,
            String metricExpression
    ) {
        super(metricField, annotation, fieldType);

        this.aggregations = aggregations;
        this.metricExpression = metricExpression;
    }

    /**
     * Returns an immutable list of supported aggregations with the first as the default aggregation.
     *
     * @return a comprehensive list of provided aggregations
     */
    List<Class<? extends Aggregation>> getAggregations() {
        return aggregations;
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

        final BaseMetric that = (BaseMetric) other;
        return getAggregations().equals(that.getAggregations())
                && getMetricExpression().equals(that.getMetricExpression());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getAggregations(), getMetricExpression());
    }

    /**
     * Returns the string representation of this {@link Metric}.
     * <p>
     * The string consists of values of all fields in the format
     * "BaseMetric[name='XXX', longName='XXX', description='XXX', dataType=XXX, defaultAggregation=XXX,
     * aggregations=XXX, YYY, ..., metricExpression='XXX']", where values can be programmatically fetched via
     * getters.
     * <p>
     * Aggregation classes are printed in its {@link Class#getSimpleName() simple name} and {@link Optional#empty()} as
     * 'N/A'
     * <p>
     * Note that there is a single space separating each value pair.
     *
     * @return serialized {@link BaseMetric}
     */
    @Override
    public String toString() {
        return new StringJoiner(", ", BaseMetric.class.getSimpleName() + "[", "]")
                .add("name='" + getName() + "'")
                .add("longName='" + getLongName() + "'")
                .add("description='" + getDescription() + "'")
                .add("dataType=" + getDataType())
                .add("aggregations="
                        + getAggregations().stream()
                                .map(Class::getSimpleName)
                                .collect(Collectors.joining(", "))
                )
                .add("metricExpression='" + this.getMetricExpression() + "'")
                .toString();
    }
}
