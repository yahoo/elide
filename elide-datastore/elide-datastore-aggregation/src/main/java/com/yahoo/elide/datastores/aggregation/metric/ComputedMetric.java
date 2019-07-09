/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metric;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.datastores.aggregation.Schema;
import com.yahoo.elide.datastores.aggregation.annotation.Meta;
import com.yahoo.elide.datastores.aggregation.annotation.MetricComputation;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;

/**
 * {@link Metric} annotated by {@link MetricComputation}.
 */
@Slf4j
public class ComputedMetric implements Metric {

    /**
     * The placeholder reserved for constructing JQPL metricExpression, which requires a table prefix.
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
     *     {@literal @}MetricComputation(metricExpression = "timeSpent / sessions")
     *     Float timeSpentPerSession
     *
     *     {@literal @}MetricComputation(metricExpression = "timeSpentPerSession / gameCount")
     *     Float timeSpentPerGame
     * }
     * }
     * </pre>
     * One possible expanded JPQL metricExpression for {@code timeSpentPerSession} can be
     * {@code com_oath_blah_FactTable.timeSpent / com_oath_blah_FactTable.sessions} and for {@code timeSpentPerGame} as
     * {@code (com_oath_blah_FactTable.timeSpent/com_oath_blah_FactTable.sessions) / com_oath_blah_FactTable.gameCount}.
     * {@link Schema} keeps such metricExpression in the form of {@code TABLE_PREFIX.timeSpent / TABLE_PREFIX.sessions}
     * for {@code timeSpentPerSession} and
     * {@code (TABLE_PREFIX.timeSpent/TABLE_PREFIX.sessions) / TABLE_PREFIX.gameCount} for
     * {@code timeSpentPerGame}, where {@code TABLE_PREFIX} is specified by {@link #TABLE_PREFIX}.
     * <p>
     * <b>It is caller's responsibility to replace the {@link #TABLE_PREFIX} with the per-user specific value at
     * runtime.</b>
     */
    public static final String TABLE_PREFIX = "<PREFIX>";

    private static final long serialVersionUID = -5243438667754088900L;

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

    public ComputedMetric(String metricField, Class<?> cls, EntityDictionary entityDictionary) {
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

        this.metricExpression = expandMetricExpression(metricField, cls, new HashSet<>(), entityDictionary);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        final ComputedMetric that = (ComputedMetric) o;
        return getName().equals(that.getName())
                && getLongName().equals(that.getLongName())
                && getDescription().equals(that.getDescription())
                && getDataType().equals(that.getDataType())
                && getAggregations().equals(that.getAggregations())
                && getMetricExpression().equals(that.getMetricExpression());
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                getName(),
                getLongName(),
                getDescription(),
                getDataType(),
                getAggregations(),
                getMetricExpression()
        );
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ComputedMetric.class.getSimpleName() + "[", "]")
                .add("name='" + name + "'")
                .add("longName='" + longName + "'")
                .add("description='" + description + "'")
                .add("dataType=" + dataType)
                .add("aggregations=" + aggregations)
                .add("metricExpression='" + metricExpression + "'")
                .toString();
    }

    /**
     * Returns the <b>expanded</b> JPQL metric metricExpression, with {@link ComputedMetric#TABLE_PREFIX table prefix
     * placeholder} attached, for computing a specified entity field.
     * <p>
     * See {@link ComputedMetric#TABLE_PREFIX} for more details.
     *
     * @param expandingField  The entity field
     * @param containingCls  The entity that contains the {@code expandingField}
     * @param visited  A set of already-visited metricExpression variables, which should be skipped during expansion.
     *
     * @return a JPQL metricExpression with placeholder
     */
    private static String expandMetricExpression(
            String expandingField,
            Class<?> containingCls,
            Set<String> visited,
            EntityDictionary entityDictionary
    ) {
        if (MetricUtils.isBaseMetric(expandingField, containingCls, entityDictionary)) {
            // hit a non-computed metric field
            return String.format("%s.%s", TABLE_PREFIX, expandingField);
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
                    && MetricUtils.isMetricField(subField, containingCls, entityDictionary)
                    && exactContain(metricExpression, subField)
            ) {
                String expandedSubField = expandMetricExpression(
                        subField,
                        containingCls,
                        visited,
                        entityDictionary
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
}
