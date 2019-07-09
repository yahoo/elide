/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metric;

import com.yahoo.elide.core.EntityDictionary;
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
public class BaseMetric implements Metric {

    public static String generateExpression(Class<? extends Aggregation> defaultAggregation) {
        try {
            Class<?> clazz = Class.forName(defaultAggregation.getCanonicalName());
            Constructor<?> ctor = clazz.getConstructor();
            Aggregation aggregation = (Aggregation) ctor.newInstance();
            return aggregation.getAggFunctionFormat();
        } catch (Exception exception) {
            String message = String.format(
                    "Cannot generate aggregation function for '%s'",
                    defaultAggregation.getCanonicalName()
            );
            log.error(message, exception);
            throw new IllegalStateException(message, exception);
        }
    }

    private static final long serialVersionUID = 3055820948480283917L;

    @Getter
    private final String name;

    @Getter
    private final String longName;

    @Getter
    private final String description;

    @Getter
    private final Class<?> dataType;

    @Getter
    private final List<Class<? extends Aggregation>> aggregations;

    @Getter
    private final String metricExpression;

    public BaseMetric(String metricField, Class<?> cls, EntityDictionary entityDictionary) {
        Meta metaData = entityDictionary.getAttributeOrRelationAnnotation(cls, Meta.class, metricField);
        Class<?> fieldType = entityDictionary.getType(cls, metricField);

        this.name = metricField;
        this.longName = metaData == null || metaData.longName().isEmpty() ? metricField : metaData.longName();
        this.description = metaData == null || metaData.description().isEmpty() ? metricField : metaData.description();
        this.dataType = fieldType;
        this.aggregations = MetricUtils.extractAggregations(metricField, cls, entityDictionary);

        if (aggregations.isEmpty()) {
            String message = String.format("'%s' in '%s' has no aggregation.", metricField, cls.getCanonicalName());
            log.error(message);
            throw new IllegalStateException(message);
        }

        this.metricExpression = generateExpression(aggregations.get(0));
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        final BaseMetric that = (BaseMetric) other;
        return getName().equals(that.getName())
                && getLongName().equals(that.getLongName())
                && getDescription().equals(that.getDescription())
                && getDataType().equals(that.getDataType())
                && getAggregations().equals(that.getAggregations())
                && this.getMetricExpression().equals(that.getMetricExpression());
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                getName(),
                getLongName(),
                getDescription(),
                getDataType(),
                getAggregations(),
                this.getMetricExpression()
        );
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
