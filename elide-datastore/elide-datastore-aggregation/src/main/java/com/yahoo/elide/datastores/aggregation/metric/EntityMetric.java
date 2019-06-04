/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metric;

import com.yahoo.elide.datastores.aggregation.annotation.MetricAggregation;
import com.yahoo.elide.datastores.aggregation.annotation.MetricComputation;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link Metric} that represents an entity field as a metric.
 * <p>
 * {@link EntityMetric} is thread-safe and can be accessed by multiple threads.
 */
@Slf4j
public class EntityMetric implements Metric {

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
    private final Class<? extends Aggregation> defaultAggregation;

    @Getter
    private final List<Class<? extends Aggregation>> allAggregations;

    private final String computedMetricExpression;

    /**
     * Constructor for simple metric.
     * <p>
     * {@link #getExpandedMetricExpression() Metric expression} is {@code null} by default.
     *
     * @param name  The entity field name as the name of this metric
     * @param longName  A human-readable name (allowing spaces) of this {@link Metric} object
     * @param description  A short description explaining the meaning of this {@link Metric}
     * @param dataType  The type of the entity field associated with this {@link Metric}
     * @param defaultAggregation  The default aggregation function to apply to this {@link Metric}
     * @param allAggregations  Complete list of supported aggregations
     *
     * @throws NullPointerException if any argument is {@code null}
     */
    public EntityMetric(
            final String name,
            final String longName,
            final String description,
            final Class<?> dataType,
            final Class<? extends Aggregation> defaultAggregation,
            final List<Class<? extends Aggregation>> allAggregations
    ) {
        this(name, longName, description, dataType, defaultAggregation, allAggregations, null);
    }

    /**
     * Constructor for computed metric.
     * <p>
     * See {@link MetricComputation}.
     *
     * @param name  The entity field name as the name of this metric
     * @param longName  A human-readable name (allowing spaces) of this {@link Metric} object
     * @param description  A short description explaining the meaning of this {@link Metric}
     * @param dataType  The type of the entity field associated with this {@link Metric}
     * @param defaultAggregation  The default aggregation function to apply to this {@link Metric}
     * @param allAggregations  Complete list of supported aggregations
     * @param computedMetricExpression  A expanded JPQL expression that represents this metric computation logic;
     * {@code null} {@link MetricAggregation simple metric}
     *
     * @throws NullPointerException if any argument, except for {@code computedMetricExpression}, is {@code null}
     */
    public EntityMetric(
            final String name,
            final String longName,
            final String description,
            final Class<?> dataType,
            final Class<? extends Aggregation> defaultAggregation,
            final List<Class<? extends Aggregation>> allAggregations,
            final String computedMetricExpression
    ) {
        this.name = Objects.requireNonNull(name, "name");
        this.longName = Objects.requireNonNull(longName, "longName");
        this.description = Objects.requireNonNull(description, "description");
        this.dataType = Objects.requireNonNull(dataType, "dataType");
        this.defaultAggregation = Objects.requireNonNull(defaultAggregation, "defaultAggregation");
        this.allAggregations = Objects.requireNonNull(allAggregations, "allAggregations");
        this.computedMetricExpression = computedMetricExpression;

        if (!allAggregations.contains(defaultAggregation)) {
            this.allAggregations.add(defaultAggregation);
        }
    }

    @Override
    public Optional<String> getExpandedMetricExpression() {
        return computedMetricExpression == null ? Optional.empty() : Optional.of(computedMetricExpression);
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }

        final EntityMetric that = (EntityMetric) other;
        return getName().equals(that.getName())
                && getLongName().equals(that.getLongName())
                && getDescription().equals(that.getDescription())
                && getDataType().equals(that.getDataType())
                && getDefaultAggregation().equals(that.getDefaultAggregation())
                && getAllAggregations().equals(that.getAllAggregations())
                && getExpandedMetricExpression().equals(that.getExpandedMetricExpression());
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                getName(),
                getLongName(),
                getDescription(),
                getDataType(),
                getDefaultAggregation(),
                getAllAggregations(),
                getExpandedMetricExpression()
        );
    }

    /**
     * Returns the string representation of this {@link Metric}.
     * <p>
     * The string consists of values of all fields in the format
     * "EntityMetric[name='XXX', longName='XXX', description='XXX', dataType=XXX, defaultAggregation=XXX,
     * allAggregations=XXX, YYY, ..., computedMetricExpression='XXX']", where values can be programmatically fetched via
     * getters.
     * <p>
     * Aggregation classes are printed in its {@link Class#getSimpleName() simple name} and {@link Optional#empty()} as
     * 'N/A'
     * <p>
     * Note that there is a single space separating each value pair.
     *
     * @return serialized {@link EntityMetric}
     */
    @Override
    public String toString() {
        return new StringJoiner(", ", EntityMetric.class.getSimpleName() + "[", "]")
                .add("name='" + getName() + "'")
                .add("longName='" + getLongName() + "'")
                .add("description='" + getDescription() + "'")
                .add("dataType=" + getDataType())
                .add("defaultAggregation=" + getDefaultAggregation().getSimpleName())
                .add("allAggregations="
                        + getAllAggregations().stream()
                                .map(Class::getSimpleName)
                                .collect(Collectors.joining(", "))
                )
                .add("computedMetricExpression='" + getExpandedMetricExpression().orElse("N/A") + "'")
                .toString();
    }
}
