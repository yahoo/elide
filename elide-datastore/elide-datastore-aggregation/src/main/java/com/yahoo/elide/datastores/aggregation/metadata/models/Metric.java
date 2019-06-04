/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata.models;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.datastores.aggregation.annotation.Meta;
import com.yahoo.elide.datastores.aggregation.annotation.MetricAggregation;
import com.yahoo.elide.datastores.aggregation.annotation.MetricFormula;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.ManyToOne;

/**
 * Column which supports aggregation.
 */
@Include(type = "metric")
@Getter
@EqualsAndHashCode(callSuper = true)
@ToString
public class Metric extends Column {
    @ManyToOne
    @ToString.Exclude
    private final MetricFunction metricFunction;

    public Metric(Table table, String fieldName, EntityDictionary dictionary) {
        super(table, fieldName, dictionary);
        Class<?> tableClass = dictionary.getEntityClass(table.getName(), table.getVersion());

        MetricAggregation aggregation = dictionary.getAttributeOrRelationAnnotation(
                tableClass,
                MetricAggregation.class,
                fieldName);

        Meta meta = dictionary.getAttributeOrRelationAnnotation(
                tableClass,
                Meta.class,
                fieldName);

        if (aggregation != null) {
            this.metricFunction = resolveAggregation(tableClass, fieldName, aggregation, meta, dictionary);
        } else {
            MetricFormula formula = dictionary.getAttributeOrRelationAnnotation(
                    tableClass,
                    MetricFormula.class,
                    fieldName);

            if (formula != null) {
                this.metricFunction = constructMetricFunction(
                        constructColumnName(tableClass, fieldName, dictionary) + "[" + fieldName + "]",
                        meta == null ? null : meta.description(),
                        formula.value(),
                        new HashSet<>());

            } else {
                throw new IllegalArgumentException("Trying to construct metric field "
                        + getId() + " without @MetricAggregation and @MetricFormula.");
            }
        }
    }

    /**
     * Resolve aggregation function from {@link MetricAggregation} annotation.
     *
     * @param tableClass table class
     * @param fieldName metric field name
     * @param aggregation aggregation annotation on the field
     * @param meta meta annotation on the field
     * @param dictionary dictionary with entity information
     * @return resolved metric function instance
     */
    private static MetricFunction resolveAggregation(Class<?> tableClass,
                                                     String fieldName,
                                                     MetricAggregation aggregation,
                                                     Meta meta,
                                                     EntityDictionary dictionary) {
        String columnName = constructColumnName(tableClass, fieldName, dictionary);
        try {
            MetricFunction metricFunction = aggregation.function().newInstance();
            metricFunction.setName(columnName + "[" + metricFunction.getName() + "]");

            if (meta != null) {
                metricFunction.setDescription(meta.description());
            }

            return metricFunction;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalArgumentException("Can't initialize function for metric " + columnName);
        }
    }

    /**
     * Dynamically construct a metric function
     *
     * @param id metric function id
     * @param description meta description
     * @param expression expression string
     * @param arguments function arguments
     * @return a metric function instance
     */
    protected MetricFunction constructMetricFunction(String id,
                                                     String description,
                                                     String expression,
                                                     Set<FunctionArgument> arguments) {
        return new MetricFunction(id, description, expression, arguments);
    }
}
