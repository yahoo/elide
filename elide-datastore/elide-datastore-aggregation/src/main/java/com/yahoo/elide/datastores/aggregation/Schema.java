/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation;

import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.datastores.aggregation.annotation.Cardinality;
import com.yahoo.elide.datastores.aggregation.annotation.CardinalitySize;
import com.yahoo.elide.datastores.aggregation.annotation.FriendlyName;
import com.yahoo.elide.datastores.aggregation.annotation.Meta;
import com.yahoo.elide.datastores.aggregation.annotation.MetricAggregation;
import com.yahoo.elide.datastores.aggregation.annotation.MetricComputation;
import com.yahoo.elide.datastores.aggregation.dimension.ColumnType;
import com.yahoo.elide.datastores.aggregation.dimension.DefaultColumnType;
import com.yahoo.elide.datastores.aggregation.dimension.DegenerateDimension;
import com.yahoo.elide.datastores.aggregation.dimension.Dimension;
import com.yahoo.elide.datastores.aggregation.dimension.EntityDimension;
import com.yahoo.elide.datastores.aggregation.metric.Aggregation;
import com.yahoo.elide.datastores.aggregation.metric.EntityMetric;
import com.yahoo.elide.datastores.aggregation.metric.Metric;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.Column;
import javax.persistence.Id;

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

    /**
     * The placeholder reserved for constructing JQPL expression, which requires a table prefix.
     * <p>
     * See {@link #getExpandedMetricExpression(String, Class, EntityDictionary, Set)}  for more details about table
     * prefixing.
     */
    public static final String TABLE_PREFIX = "<PREFIX>";

    private static final String NON_METRIC_ERROR_FORMAT = "'%s' is not a metric";

    /**
     * The default cardinality for entity without {@link Cardinality} annotation.
     *
     * @return the default table size backing this {@link Dimension}
     */
    protected static CardinalitySize getDefaultCardinality() {
        return CardinalitySize.LARGE;
    }

    @Getter
    private final Class<?> entityClass;

    @Getter
    private final Set<Metric> metrics;

    @Getter
    private final Set<Dimension> dimensions;

    @Getter(value = AccessLevel.PRIVATE)
    private final EntityDictionary entityDictionary;

    /**
     * Returns whether or not an entity field is a simple metric, i.e. a metric field not decorated by
     * {@link MetricComputation}.
     *
     * @param fieldName  The entity field
     * @param containingClz  The entity containing the field
     * @param entityDictionary  An object that offers meta info about entity and fields, such as annotation and field
     * names
     *
     * @return {@code true} if the field is a metric field and also a base metric without JPQL expression annotated
     */
    public static boolean isBaseMetric(String fieldName, Class<?> containingClz, EntityDictionary entityDictionary) {
        if (!isMetricField(fieldName, containingClz, entityDictionary)) {
            return false;
        }

        return !entityDictionary.attributeOrRelationAnnotationExists(containingClz, fieldName, MetricComputation.class);
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
     * @param containingCls  The entity that contains the {@code fieldName}
     * @param entityDictionary  An object that offers meta info about entity and fields, such as annotation and field
     * names
     *
     * @return {@code true} if the field is a metric field
     */
    public static boolean isMetricField(String fieldName, Class<?> containingCls, EntityDictionary entityDictionary) {
        return entityDictionary.attributeOrRelationAnnotationExists(
                containingCls, fieldName, MetricAggregation.class
        )
                || entityDictionary.attributeOrRelationAnnotationExists(
                containingCls, fieldName, MetricComputation.class
        );
    }

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

        this.metrics = getAllMetrics(entityDictionary);
        this.dimensions = getAllDimensions(entityDictionary);
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
        Meta metaData = entityDictionary.getAttributeOrRelationAnnotation(cls, Meta.class, metricField);
        Class<?> fieldType = entityDictionary.getType(cls, metricField);

        return isBaseMetric(metricField, cls, entityDictionary)
                ? new EntityMetric(
                        metricField,
                        metaData == null || metaData.longName().isEmpty() ? metricField : metaData.longName(),
                        metaData == null || metaData.description().isEmpty() ? metricField : metaData.description(),
                        fieldType,
                        extractDefaultAggregation(metricField, cls, entityDictionary),
                        extractNonDefaultAggregations(metricField, cls, entityDictionary)
                )
                : new EntityMetric(
                        metricField,
                        metaData == null || metaData.longName().isEmpty() ? metricField : metaData.longName(),
                        metaData == null || metaData.description().isEmpty() ? metricField : metaData.description(),
                        fieldType,
                        extractDefaultAggregation(metricField, cls, entityDictionary),
                        extractNonDefaultAggregations(metricField, cls, entityDictionary),
                        getExpandedMetricExpression(metricField, cls, entityDictionary, new HashSet<>())
                );
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
                ? constructEntityDimension(dimensionField, cls, entityDictionary)
                : constructDegenerateDimension(dimensionField, cls, entityDictionary);
    }

    /**
     * Constructs all metrics found in an entity.
     * <p>
     * This method calls {@link #constructMetric(String, Class, EntityDictionary)} to create each dimension inside the
     * entity
     *
     * @param entityDictionary  An object that offers meta info about entity and fields, such as annotation and field
     * names
     *
     * @return all metric fields as {@link Metric} objects
     */
    private Set<Metric> getAllMetrics(EntityDictionary entityDictionary) {
        return entityDictionary.getAllFields(getEntityClass()).stream()
                .filter(field -> isMetricField(field, getEntityClass(), entityDictionary))
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
     * @param entityDictionary  An object that offers meta info about entity and fields, such as annotation and field
     * names
     *
     * @return all non-metric fields as {@link Dimension} objects
     */
    private Set<Dimension> getAllDimensions(EntityDictionary entityDictionary) {
        return entityDictionary.getAllFields(getEntityClass()).stream()
                .filter(field -> !isMetricField(field, getEntityClass(), entityDictionary))
                .map(field -> constructDimension(field, getEntityClass(), entityDictionary))
                .collect(
                        Collectors.collectingAndThen(
                                Collectors.toSet(),
                                Collections::unmodifiableSet
                        )
                );
    }

    /**
     * Parses to obtain the default aggregation function to apply to this {@link Metric}.
     *
     * @param fieldName  The entity field
     * @param containingCls  The entity that contains the {@code fieldName}
     * @param entityDictionary  An object that offers meta info about entity and fields, such as annotation and field
     * names
     *
     * @return default aggregation class
     *
     * @throws IllegalArgumentException if the {@code fieldName} is not a metric field
     */
    private static Class<? extends Aggregation> extractDefaultAggregation(
            String fieldName,
            Class<?> containingCls,
            EntityDictionary entityDictionary
    ) {
        if (!isMetricField(fieldName, containingCls, entityDictionary)) {
            String message = String.format(NON_METRIC_ERROR_FORMAT, fieldName);
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        return entityDictionary.getAttributeOrRelationAnnotation(containingCls, MetricAggregation.class, fieldName)
                .defaultAggregation();
    }

    /**
     * Parses to obtain all supported aggregation functions to apply to this {@link Metric}.
     *
     * @param fieldName  The entity field
     * @param containingCls  The entity that contains the {@code fieldName}
     * @param entityDictionary  An object that offers meta info about entity and fields, such as annotation and field
     * names
     *
     * @return all available aggregation classes
     *
     * @throws IllegalArgumentException if the {@code fieldName} is not a metric field
     */
    private static List<Class<? extends Aggregation>> extractNonDefaultAggregations(
            String fieldName,
            Class<?> containingCls,
            EntityDictionary entityDictionary
    ) {
        if (!isMetricField(fieldName, containingCls, entityDictionary)) {
            String message = String.format(NON_METRIC_ERROR_FORMAT, fieldName);
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        return Arrays.stream(
                entityDictionary.getAttributeOrRelationAnnotation(
                        containingCls,
                        MetricAggregation.class,
                        fieldName
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
     * Returns the <b>expanded</b> JPQL metric expression, with {@link #TABLE_PREFIX table prefix placeholder}
     * attached, for computing a specified entity field.
     * <p>
     * For example, consider the following entity with {@code expandingField = timeSpentPerSession or timeSpentPerGame}
     * and {@code containingCls = FactTable.class}
     * <pre>
     * {@code
     * public class FactTable {
     *
     *     {@literal @}MetricAggregation(sum.class)
     *     Long sessions
     *
     *     {@literal @}MetricAggregation(sum.class)
     *     Long timeSpent
     *
     *     {@literal @}MetricComputation(expression = "timeSpent / sessions")
     *     Float timeSpentPerSession
     *
     *     {@literal @}MetricComputation(expression = "timeSpentPerSession / gameCount")
     *     Float timeSpentPerGame
     * }
     * }
     * </pre>
     * One possible expanded JPQL expression for {@code timeSpentPerSession} can be
     * {@code com_oath_blah_FactTable.timeSpent / com_oath_blah_FactTable.sessions} and for {@code timeSpentPerGame} as
     * {@code (com_oath_blah_FactTable.timeSpent/com_oath_blah_FactTable.sessions) / com_oath_blah_FactTable.gameCount}.
     * This method, in this case, returns {@code TABLE_PREFIX.timeSpent / TABLE_PREFIX.sessions} for
     * {@code timeSpentPerSession} and
     * {@code (TABLE_PREFIX.timeSpent/TABLE_PREFIX.sessions) / TABLE_PREFIX.gameCount} for
     * {@code timeSpentPerGame}, where {@code TABLE_PREFIX} is specified by {@link #TABLE_PREFIX}.
     * <p>
     * <b>It is caller's responsibility to replace the {@link #TABLE_PREFIX} with the per-user specific value at
     * runtime.</b>
     *n
     * @param expandingField  The entity field
     * @param containingCls  The entity that contains the {@code expandingField}
     * @param entityDictionary  An object that offers meta info about entity and fields, such as annotation and field
     * names
     * @param visited  A set of already-visited expression variables, which should be skipped during expansion.
     *
     * @return a JPQL expression with placeholder
     */
    private static String getExpandedMetricExpression(
            String expandingField,
            Class<?> containingCls,
            EntityDictionary entityDictionary,
            Set<String> visited
    ) {
        if (!entityDictionary.isValidField(containingCls, expandingField)) {
            // hit a non-existing field
            String message = String.format(
                    "'%s' not found in '%s'",
                    expandingField,
                    containingCls.getName()
            );
            log.error(message);
            throw new IllegalStateException(message);
        }
        if (!isMetricField(expandingField, containingCls, entityDictionary)) {
            // hit a non-metric field.
            String message = String.format(
                    "'%s' in '%s' is not a defined as a metric",
                    expandingField,
                    containingCls.getName()
            );
            log.error(message);
            throw new IllegalStateException(message);
        }

        if (isBaseMetric(expandingField, containingCls, entityDictionary)) {
            // hit a non-computed metric field
            return String.format(
                    "%s.%s",
                    TABLE_PREFIX,
                    getMappedColumn(expandingField, containingCls, entityDictionary)
            );
        }

        // hit a computed metric field if valid. Expand
        visited.add(expandingField);
        String metricExpression = entityDictionary.getAttributeOrRelationAnnotation(
                containingCls,
                MetricComputation.class,
                expandingField
        ).expression();

        for (String subField : entityDictionary.getAllFields(containingCls)) {
            if (!visited.contains(subField)
                    && !subField.equals(expandingField)
                    && isMetricField(subField, containingCls, entityDictionary)
                    && exactContain(metricExpression, subField)
            ) {
                String expandedSubField = getExpandedMetricExpression(
                        subField,
                        containingCls,
                        entityDictionary,
                        visited
                );

                metricExpression = metricExpression.replace(
                        subField,
                        expandedSubField
                );
            }
        }

        return metricExpression;
    }

    /**
     * Returns whether or not a substring is exactly contained in another string.
     * <p>
     * For example, "timeSpent" is not exactly contained in "(timeSpentPerGame1 + timeSpentPerGame2) / 2", but
     * "timeSpentPerGame1" is exactly contained.
     *
     * @param big  The another string
     * @param small  The substring
     *
     * @return {@code true} if there is an exact contain
     */
    private static boolean exactContain(String big, String small) {
        return Arrays.stream(big.split("[ -+*/()]")).anyMatch(small::equals);
    }

    /**
     * Returns the JPA mapped DB column backing an entity field.
     * <p>
     * If the field is not decorated by the JPA or is decorated but with no column alias, the entity field name itself
     * is returned; otherwise the mapped column name is returned.
     * <p>
     * For example,
     * <pre>
     * {@code
     *     {@literal @}Column(name = "rounds")
     *     Long sessions                       // return "rounds"
     *
     *     {@literal @}Column
     *     Long timeSpent                      // return "timeSpent"
     *
     *     Float timeSpentPerSession           // return "timeSpentPerSession"
     * }
     * </pre>
     *
     * @param fieldName  The entity field
     * @param containingCls  The entity that contains the {@code fieldName}
     * @param entityDictionary  An object that offers meta info about entity and fields, such as annotation and field
     * names
     *
     * @return a mapped column alias or field name itself
     */
    private static String getMappedColumn(String fieldName, Class<?> containingCls, EntityDictionary entityDictionary) {
        Column annotation = entityDictionary.getAttributeOrRelationAnnotation(containingCls, Column.class, fieldName);
        return annotation == null || annotation.name().isEmpty() ? fieldName : annotation.name();
    }

    /**
     * Returns a new instance of dimension backed by a table.
     *
     * @param dimensionField  The dimension out of which the table dimension is to be created
     * @param cls  The entity that contains the {@code dimensionField}
     * @param entityDictionary  An object that offers meta info about entity and fields, such as annotation and field
     * names
     *
     * @return a {@link Dimension} instance on sub-type {@link EntityDimension}
     */
    private static Dimension constructEntityDimension(
            String dimensionField,
            Class<?> cls,
            EntityDictionary entityDictionary
    ) {
        Meta metaData = entityDictionary.getAttributeOrRelationAnnotation(cls, Meta.class, dimensionField);
        Class<?> fieldType = entityDictionary.getType(cls, dimensionField);

        return new EntityDimension(
                dimensionField,
                metaData == null || metaData.longName().isEmpty() ? dimensionField : metaData.longName(),
                metaData == null || metaData.description().isEmpty() ? dimensionField : metaData.description(),
                fieldType,
                getEstimatedCardinality(dimensionField, cls, entityDictionary),
                getFriendlyNameField(cls, entityDictionary)
        );
    }

    /**
     * Returns a new instance of degenerate dimension.
     *
     * @param dimensionField  The dimension out of which the degenerate dimension is to be created
     * @param cls  The entity that contains the {@code dimensionField}
     * @param entityDictionary  An object that offers meta info about entity and fields, such as annotation and field
     * names
     *
     * @return a {@link Dimension} instance on sub-type {@link DegenerateDimension}
     */
    private static Dimension constructDegenerateDimension(
            String dimensionField,
            Class<?> cls,
            EntityDictionary entityDictionary
    ) {
        Meta metaData = entityDictionary.getAttributeOrRelationAnnotation(cls, Meta.class, dimensionField);
        Class<?> fieldType = entityDictionary.getType(cls, dimensionField);

        return new DegenerateDimension(
                dimensionField,
                metaData == null || metaData.longName().isEmpty() ? dimensionField : metaData.longName(),
                metaData == null || metaData.description().isEmpty() ? dimensionField : metaData.description(),
                fieldType,
                getEstimatedCardinality(dimensionField, cls, entityDictionary),
                dimensionField,
                parseColumnType(dimensionField, cls, entityDictionary)
        );
    }

    /**
     * Returns the nature of a SQL column that is backing a specified dimension field in an entity.
     *
     * @param dimensionField  The provided dimension field
     * @param cls  The entity having the {@code dimensionField}
     * @param entityDictionary  An object that offers meta info about entity and fields, such as annotation and field
     * names
     *
     * @return a {@link ColumnType}, such as PK or a regular field
     */
    private static ColumnType parseColumnType(String dimensionField, Class<?> cls, EntityDictionary entityDictionary) {
        if (entityDictionary.attributeOrRelationAnnotationExists(cls, dimensionField, Id.class)) {
            return DefaultColumnType.PRIMARY_KEY;
        } else {
            return DefaultColumnType.FIELD;
        }
    }

    /**
     * Returns the entity field that is defined to be a human displayable column of that entity.
     *
     * @param cls  The entity or a relation
     * @param entityDictionary  An object that offers meta info about entity and fields, such as annotation and field
     * names
     *
     * @return friendlyName of the entity
     *
     * @throws IllegalStateException if more than 1 fields are annotated by the {@link FriendlyName}
     */
    private static String getFriendlyNameField(Class<?> cls, EntityDictionary entityDictionary) {
        List<String> singleFriendlyName = entityDictionary.getAllFields(cls).stream()
                .filter(field -> entityDictionary.attributeOrRelationAnnotationExists(
                        cls,
                        field,
                        FriendlyName.class
                ))
                .collect(Collectors.toList());

        if (singleFriendlyName.size() > 1) {
            String message = String.format(
                    "Multiple @FriendlyName fields found in entity '%s'. Can only have 0 or 1",
                    cls.getName()
            );
            log.error(message);
            throw new IllegalStateException(message);
        }

        return singleFriendlyName.isEmpty()
                ? entityDictionary.getIdFieldName(cls) // no friendly name found; use @Id field as friendly name
                : singleFriendlyName.get(0);
    }

    /**
     * Returns the estimated cardinality of this a dimension field.
     * <p>
     * {@link #getDefaultCardinality() Default} is returned when the dimension is not annotated by {@link Cardinality}.
     *
     * @param dimension  The dimension field
     * @param cls  The entity name of the dimension from which estimated cardinality is being looked up
     * @param entityDictionary  An object that offers meta info about entity and fields, such as annotation and field
     * names
     *
     * @return cardinality of the dimension field
     */
    private static CardinalitySize getEstimatedCardinality(
            String dimension,
            Class<?> cls,
            EntityDictionary entityDictionary
    ) {
        if (entityDictionary.isRelation(cls, dimension)) {
            // try to get annotation from entity first
            Cardinality annotation = entityDictionary.getAnnotation(
                    entityDictionary.getType(cls, dimension),
                    Cardinality.class
            );

            if (annotation != null) {
                return annotation.size();
            }

            // annotation is not on entity; then must be on field or method
        }

        Cardinality annotation = entityDictionary
                .getAttributeOrRelationAnnotation(cls, Cardinality.class, dimension);

        return annotation == null
                ? getDefaultCardinality() // no cardinality specified on field or method; use default then
                : annotation.size();
    }
}
