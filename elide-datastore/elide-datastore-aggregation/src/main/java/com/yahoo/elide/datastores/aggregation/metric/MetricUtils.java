/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metric;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.datastores.aggregation.annotation.MetricAggregation;
import com.yahoo.elide.datastores.aggregation.annotation.MetricComputation;

import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Assorted methods for constructing and using {@link Metric} instances
 */
@Slf4j
public final class MetricUtils {

    /**
     * Constructor.
     * <p>
     * Suppress default constructor for noninstantiability.
     */
    private MetricUtils() {
        throw new AssertionError();
    }

    /**
     * Returns whether or not an entity field is a simple metric, i.e. a metric field not decorated by
     * {@link MetricComputation}.
     *
     * @param fieldName  The entity field
     * @param containingClz  The entity containing the field
     *
     * @return {@code true} if the field is a metric field and also a base metric without JPQL expression annotated
     */
    public static boolean isBaseMetric(String fieldName, Class<?> containingClz, EntityDictionary entityDictionary) {
        if (!isMetricField(fieldName, containingClz, entityDictionary)) {
            return false;
        }

        return !entityDictionary.attributeOrRelationAnnotationExists(
                containingClz,
                fieldName,
                MetricComputation.class
        );
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
     *
     * @return {@code true} if the field is a metric field
     */
    public static boolean isMetricField(String fieldName, Class<?> containingCls,  EntityDictionary entityDictionary) {
        return entityDictionary.attributeOrRelationAnnotationExists(
                containingCls, fieldName, MetricAggregation.class
        )
                || entityDictionary.attributeOrRelationAnnotationExists(
                containingCls, fieldName, MetricComputation.class
        );
    }

    /**
     * Parses to obtain all supported aggregation functions to apply to this {@link Metric}.
     *
     * @param fieldName  The entity field
     * @param containingCls  The entity that contains the {@code fieldName}
     *
     * @return all available aggregation classes
     *
     * @throws IllegalArgumentException if the {@code fieldName} is not a metric field
     */
    public static List<Class<? extends Aggregation>> extractAggregations(
            String fieldName,
            Class<?> containingCls,
            EntityDictionary entityDictionary
    ) {
        if (!isMetricField(fieldName, containingCls, entityDictionary)) {
            String message = String.format("'%s' is not a metric", fieldName);
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
}
