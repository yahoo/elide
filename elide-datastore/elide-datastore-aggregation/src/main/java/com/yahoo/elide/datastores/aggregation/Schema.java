/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation;

import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.datastores.aggregation.annotation.CardinalitySize;
import com.yahoo.elide.datastores.aggregation.annotation.Temporal;
import com.yahoo.elide.datastores.aggregation.dimension.DegenerateDimension;
import com.yahoo.elide.datastores.aggregation.dimension.Dimension;
import com.yahoo.elide.datastores.aggregation.dimension.EntityDimension;
import com.yahoo.elide.datastores.aggregation.dimension.TimeDimension;
import com.yahoo.elide.datastores.aggregation.metric.BaseMetric;
import com.yahoo.elide.datastores.aggregation.metric.ComputedMetric;
import com.yahoo.elide.datastores.aggregation.metric.Metric;
import com.yahoo.elide.datastores.aggregation.metric.MetricUtils;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Collectors;

/**
 * {@link Schema} keeps track of table, metrics, and dimensions of an entity for AggregationDataStore.
 * <p>
 * On calling {@link DataStore#populateEntityDictionary(EntityDictionary)}, one {@link Schema} will be created for each
 * entity.
 * <p>
 * By overriding {@link #constructDimension(String, Class, EntityDictionary)} and
 * {@link #constructMetric(String, Class, EntityDictionary)}, people can have new schema backed by their own defined
 * {@link Dimension}s and {@link Metric}s.
 * <p>
 * {@link Schema} is thread-safe and can be accessed by multiple-threads.
 */
@Slf4j
public class Schema {

    @Getter
    private final Class<?> entityClass;

    @Getter
    private final Set<Metric> metrics;

    @Getter
    private final Set<Dimension> dimensions;

    @Getter(value = AccessLevel.PRIVATE)
    private final EntityDictionary entityDictionary;

    /**
     * Private constructor which avoids itself being overridden.
     * <p>
     * This constructor calls {@link #constructDimension(String, Class, EntityDictionary)} and
     * {@link #constructMetric(String, Class, EntityDictionary)} ()} to construct all {@link Dimension}s and
     * {@link Metric}s associated with the entity class passed in.
     *
     * @param cls  The type of the entity, whose {@link Schema} is to be constructed
     * @param entityDictionary  The meta info object that helps to construct {@link Schema}
     *
     * @throws NullPointerException if anyone of the arguments is {@code null}
     */
    public Schema(Class<?> cls, EntityDictionary entityDictionary) {
        this.entityClass = Objects.requireNonNull(cls, "cls");
        this.entityDictionary = Objects.requireNonNull(entityDictionary, "entityDictionary");

        this.metrics = getAllMetrics();
        this.dimensions = getAllDimensions();
    }

    /**
     * Returns an {@link Optional} of the JPQL DB table size associated with a dimension field, or
     * {@link Optional#empty()} if no such dimension field is found.
     *
     * @param fieldName  A field whose corresponding table size is to be estimated
     *
     * @return {@link Optional} dimension size as {@link CardinalitySize} or {@link Optional#empty()}
     */
    public Optional<CardinalitySize> getDimensionSize(String fieldName) {
        return getDimensions().stream()
                .filter(dimension -> dimension.getName().equals(fieldName))
                .map(Dimension::getCardinality)
                .findAny();
    }

    /**
     * Constructs a new {@link Metric} instance.
     *
     * @param metricField  The entity field of the metric being constructed
     * @param cls  The entity that contains the metric being constructed
     * @param entityDictionary  The auxiliary object that offers binding info used to construct this
     * {@link Metric}
     *
     * @return a {@link Metric}
     */
    protected Metric constructMetric(String metricField, Class<?> cls, EntityDictionary entityDictionary) {
        return MetricUtils.isBaseMetric(metricField, cls, entityDictionary)
                ? new BaseMetric(metricField, cls, entityDictionary)
                : new ComputedMetric(metricField, cls, entityDictionary);
    }

    /**
     * Constructs and returns a new instance of {@link Dimension}.
     *
     * @param dimensionField  The entity field of the dimension being constructed
     * @param cls  The entity that contains the dimension being constructed
     * @param entityDictionary  The auxiliary object that offers binding info used to construct this
     * {@link Dimension}
     *
     * @return a {@link Dimension}
     */
    protected Dimension constructDimension(String dimensionField, Class<?> cls, EntityDictionary entityDictionary) {
        return entityDictionary.isRelation(cls, dimensionField)
                ? constructEntityDimension(dimensionField, cls)
                : constructDegenerateDimension(dimensionField, cls);
    }

    /**
     * Constructs all metrics found in an entity.
     * <p>
     * This method calls {@link #constructMetric(String, Class, EntityDictionary)} to create each dimension inside the
     * entity
     *
     * @return all metric fields as {@link Metric} objects
     */
    private Set<Metric> getAllMetrics() {
        return entityDictionary.getAllFields(getEntityClass()).stream()
                .filter(field -> MetricUtils.isMetricField(field, getEntityClass(), getEntityDictionary()))
                .map(field -> constructMetric(field, getEntityClass(), entityDictionary))
                .collect(
                        Collectors.collectingAndThen(
                                Collectors.toSet(),
                                Collections::unmodifiableSet
                        )
                );
    }

    /**
     * Constructs all dimensions found in an entity.
     * <p>
     * This method calls {@link #constructDimension(String, Class, EntityDictionary)} to create each dimension inside
     * the entity
     *
     * @return all non-metric fields as {@link Dimension} objects
     */
    private Set<Dimension> getAllDimensions() {
        return getEntityDictionary().getAllFields(getEntityClass()).stream()
                .filter(field -> !MetricUtils.isMetricField(field, getEntityClass(), getEntityDictionary()))
                .map(field -> constructDimension(field, getEntityClass(), getEntityDictionary()))
                .collect(
                        Collectors.collectingAndThen(
                                Collectors.toSet(),
                                Collections::unmodifiableSet
                        )
                );
    }

    /**
     * Returns a new instance of dimension backed by a table.
     *
     * @param dimensionField  The dimension out of which the table dimension is to be created
     * @param cls  The entity that contains the {@code dimensionField}
     *
     * @return a {@link Dimension} instance on sub-type {@link EntityDimension}
     */
    private Dimension constructEntityDimension(String dimensionField, Class<?> cls) {
        return new EntityDimension(dimensionField, cls, getEntityDictionary());
    }

    /**
     * Returns a new instance of degenerate dimension.
     *
     * @param dimensionField  The dimension out of which the degenerate dimension is to be created
     * @param cls  The entity that contains the {@code dimensionField}
     *
     * @return a {@link Dimension} instance on sub-type {@link DegenerateDimension}
     */
    private Dimension constructDegenerateDimension(String dimensionField, Class<?> cls) {
        return getEntityDictionary().attributeOrRelationAnnotationExists(cls, dimensionField, Temporal.class)
                ? createTimeDimension(dimensionField, cls) // temporal column
                : createDegenerateDimension(dimensionField, cls);
    }

    /**
     * Returns a new instance of degenerate dimension.
     *
     * @param dimensionField  The dimension out of which the degenerate dimension is to be created
     * @param cls  The entity that contains the {@code dimensionField}
     *
     * @return a {@link Dimension} instance on sub-type {@link DegenerateDimension}
     */
    private Dimension createDegenerateDimension(String dimensionField, Class<?> cls) {
        return new DegenerateDimension(dimensionField, cls, getEntityDictionary());
    }

    /**
     * Returns a new instance of time dimension.
     *
     * @param dimensionField  The dimension out of which the degenerate dimension is to be created
     * @param cls  The entity that contains the {@code dimensionField}
     *
     * @return a {@link Dimension} instance on sub-type {@link TimeDimension}
     */
    private Dimension createTimeDimension(String dimensionField, Class<?> cls) {
        Temporal temporal = getEntityDictionary().getAttributeOrRelationAnnotation(cls, Temporal.class, dimensionField);

        return new TimeDimension(
                dimensionField,
                cls,
                getEntityDictionary(),
                TimeZone.getTimeZone(temporal.timeZone()),
                temporal.timeGrain()
        );
    }
}
