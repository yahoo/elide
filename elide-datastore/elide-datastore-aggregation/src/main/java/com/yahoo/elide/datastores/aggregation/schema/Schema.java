/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.schema;

import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.filter.FilterPredicate;
import com.yahoo.elide.datastores.aggregation.annotation.CardinalitySize;
import com.yahoo.elide.datastores.aggregation.annotation.Meta;
import com.yahoo.elide.datastores.aggregation.annotation.MetricAggregation;
import com.yahoo.elide.datastores.aggregation.annotation.MetricComputation;
import com.yahoo.elide.datastores.aggregation.annotation.Temporal;
import com.yahoo.elide.datastores.aggregation.schema.dimension.DimensionColumn;
import com.yahoo.elide.datastores.aggregation.schema.dimension.TimeDimensionColumn;
import com.yahoo.elide.datastores.aggregation.schema.dimension.impl.DegenerateDimension;
import com.yahoo.elide.datastores.aggregation.schema.dimension.impl.EntityDimension;
import com.yahoo.elide.datastores.aggregation.schema.dimension.impl.TimeDimension;
import com.yahoo.elide.datastores.aggregation.schema.metric.AggregatedMetric;
import com.yahoo.elide.datastores.aggregation.schema.metric.Aggregation;
import com.yahoo.elide.datastores.aggregation.schema.metric.Metric;
import com.google.common.collect.Sets;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * {@link Schema} keeps track of table, metrics, and dimensions of an entity for AggregationDataStore.
 * <p>
 * On calling {@link DataStore#populateEntityDictionary(EntityDictionary)}, one {@link Schema} will be created for each
 * entity.
 * <p>
 * By overriding {@link #constructDimension(String, Class, EntityDictionary)} and
 * {@link #constructMetric(String, Class, EntityDictionary)}, people can have new schema backed by their own defined
 * {@link DimensionColumn}s and {@link Metric}s.
 * <p>
 * {@link Schema} is thread-safe and can be accessed by multiple-threads.
 */
@Slf4j
public class Schema {

    @Getter
    protected final Class<?> entityClass;
    @Getter
    protected final Set<Metric> metrics;
    protected final Map<String, DimensionColumn> dimensions;
    @Getter(value = AccessLevel.PROTECTED)
    protected final EntityDictionary entityDictionary;

    /**
     * Constructor
     * <p>
     * This constructor calls {@link #constructDimension(String, Class, EntityDictionary)} and
     * {@link #constructMetric(String, Class, EntityDictionary)} ()} to construct all {@link DimensionColumn}s and
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
     * Returns an immutable view of all {@link DimensionColumn}s and {@link Metric}s described by this {@link Schema}.
     *
     * @return union of all {@link DimensionColumn}s and {@link Metric}s under this {@link Schema}
     */
    public Set<Column> getAllColumns() {
        return Stream.concat(getMetrics().stream(), getDimensions().stream())
                .map(item -> (Column) item)
                .collect(
                        Collectors.collectingAndThen(
                                Collectors.toSet(),
                                Collections::unmodifiableSet
                        )
                );
    }

    /**
     * Finds the {@link DimensionColumn} by name.
     *
     * @param dimensionName  The entity field name associated with the searched {@link DimensionColumn}
     *
     * @return {@link DimensionColumn} found or {@code null} if not found
     */
    public DimensionColumn getDimension(String dimensionName) {
        return dimensions.get(dimensionName);
    }

    /**
     * Finds the {@link TimeDimensionColumn} by name.
     *
     * @param dimensionName  The entity field name associated with the searched {@link TimeDimensionColumn}
     *
     * @return {@link TimeDimensionColumn} found or {@code null} if not found
     */
    public TimeDimensionColumn getTimeDimension(String dimensionName) {
        DimensionColumn dimension = dimensions.get(dimensionName);

        if (dimension instanceof TimeDimensionColumn) {
            return (TimeDimensionColumn) dimension;
        }

        return null;
    }

    /**
     * Returns the complete list of time dimensions.
     * @return the complete list of time dimensions.
     */
    public List<TimeDimensionColumn> getTimeDimensions() {
        return dimensions.values().stream()
                .filter(dim -> dim instanceof TimeDimensionColumn)
                .map(TimeDimensionColumn.class::cast)
                .collect(Collectors.toList());
    }

    /**
     * Finds the {@link Metric} by name.
     *
     * @param metricName  The entity field name associated with the searched {@link Metric}
     *
     * @return {@link Metric} found or {@code null} if not found
     */
    public Metric getMetric(String metricName) {
        return getMetrics().stream()
                .filter(metric -> metric.getName().equals(metricName))
                .findAny()
                .orElse(null);
    }

    /**
     * Returns whether or not an entity field is a metric field.
     * <p>
     * A field is a metric field iff that field is annotated by at least one of
     * <ol>
     *     <li> {@link MetricAggregation}
     *     <li> {@link MetricComputation}
     * </ol>
     *
     * @param fieldName  The entity field
     *
     * @return {@code true} if the field is a metric field
     */
    public boolean isMetricField(String fieldName) {
        return getEntityDictionary().attributeOrRelationAnnotationExists(
                getEntityClass(), fieldName, MetricAggregation.class
        )
                || getEntityDictionary().attributeOrRelationAnnotationExists(
                getEntityClass(), fieldName, MetricComputation.class
        );
    }

    /**
     * An alias to assign this schema.
     * @return an alias that can be used in SQL.
     */
    public String getAlias() {
        return FilterPredicate.getTypeAlias(entityClass);
    }

    /**
     * Returns the set of all dimensions.
     * @return The set of all dimensions.
     */
    public Set<DimensionColumn> getDimensions() {
        return dimensions.values().stream().collect(Collectors.toSet());
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
        Meta metaData = entityDictionary.getAttributeOrRelationAnnotation(cls, Meta.class, metricField);
        Class<?> fieldType = entityDictionary.getType(cls, metricField);

        List<Class<? extends Aggregation>> aggregations = getAggregations(metricField, cls, entityDictionary);

        return new AggregatedMetric(
                this,
                metricField,
                metaData,
                fieldType,
                aggregations
        );
    }

    /**
     * Retrieve all supported aggregations for a metric.
     *
     * @param metricField  The entity field
     * @param cls  The entity class
     * @param entityDictionary  The auxiliary object that offers binding information
     *
     * @return list of supported aggregations
     */
    protected static List<Class<? extends Aggregation>> getAggregations(
            String metricField,
            Class<?> cls,
            EntityDictionary entityDictionary
    ) {
        return Arrays.stream(
                entityDictionary.getAttributeOrRelationAnnotation(
                        cls,
                        MetricAggregation.class,
                        metricField
                ).aggregations()
        )
                .collect(
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                Collections::unmodifiableList
                        )
                );
    }

    /**
     * Constructs and returns a new instance of {@link DimensionColumn}.
     *
     * @param dimensionField  The entity field of the dimension being constructed
     * @param cls  The entity that contains the dimension being constructed
     * @param entityDictionary  The auxiliary object that offers binding info used to construct this
     * {@link DimensionColumn}
     *
     * @return a {@link DimensionColumn}
     */
    protected DimensionColumn constructDimension(String dimensionField,
                                                 Class<?> cls, EntityDictionary entityDictionary) {
        // field with ToMany relationship is not supported
        if (getEntityDictionary().getRelationshipType(cls, dimensionField).isToMany()) {
            String message = String.format("ToMany relationship is not supported in '%s'", cls.getCanonicalName());
            log.error(message);
            throw new IllegalStateException(message);
        }

        Meta metaData = entityDictionary.getAttributeOrRelationAnnotation(cls, Meta.class, dimensionField);
        Class<?> fieldType = entityDictionary.getType(cls, dimensionField);

        String friendlyName = EntityDimension.getFriendlyNameField(cls, entityDictionary);
        CardinalitySize cardinality = EntityDimension.getEstimatedCardinality(dimensionField, cls, entityDictionary);

        if (entityDictionary.isRelation(cls, dimensionField)) {
            // relationship field
            return new EntityDimension(
                    this,
                    dimensionField,
                    metaData,
                    fieldType,
                    cardinality,
                    friendlyName
            );
        } else if (!getEntityDictionary().attributeOrRelationAnnotationExists(cls, dimensionField, Temporal.class)) {
            // regular field
            return new DegenerateDimension(
                    this,
                    dimensionField,
                    metaData,
                    fieldType,
                    cardinality,
                    friendlyName,
                    DegenerateDimension.parseColumnType(dimensionField, cls, entityDictionary)
            );
        } else {
            // temporal field
            Temporal temporal = getEntityDictionary().getAttributeOrRelationAnnotation(
                    cls,
                    Temporal.class,
                    dimensionField
            );

            return new TimeDimension(
                    this,
                    dimensionField,
                    metaData,
                    fieldType,
                    cardinality,
                    friendlyName,
                    TimeZone.getTimeZone(temporal.timeZone()),
                    Sets.newHashSet(temporal.grains())
            );

        }
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
        return getEntityDictionary().getAllFields(getEntityClass()).stream()
                .filter(this::isMetricField)
                // TODO: remove the filter() below when computedMetric is supported
                .filter(field ->
                        getEntityDictionary()
                                .attributeOrRelationAnnotationExists(getEntityClass(), field, MetricAggregation.class)
                )
                .map(field -> constructMetric(field, getEntityClass(), getEntityDictionary()))
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
     * @return all non-metric fields as {@link DimensionColumn} objects
     */
    private Map<String, DimensionColumn> getAllDimensions() {
        return getEntityDictionary().getAllFields(getEntityClass()).stream()
                .filter(field -> !isMetricField(field))
                .map(field -> constructDimension(field, getEntityClass(), getEntityDictionary()))
                .collect(Collectors.toMap(DimensionColumn::getName, Function.identity()));
    }
}
