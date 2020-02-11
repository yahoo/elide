/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata.models;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.datastores.aggregation.annotation.Meta;
import com.yahoo.elide.datastores.aggregation.annotation.MetricAggregation;
import com.yahoo.elide.datastores.aggregation.metadata.enums.Format;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Set;
import javax.persistence.ManyToOne;

/**
 * Column which supports aggregation.
 */
@EqualsAndHashCode(callSuper = true)
@Include(type = "metric")
@Data
public class Metric extends Column {
    private Format defaultFormat;

    @ManyToOne
    @ToString.Exclude
    private MetricFunction metricFunction;

    public Metric(Class<?> tableClass, String fieldName, EntityDictionary dictionary) {
        super(tableClass, fieldName, dictionary);

        MetricAggregation metric = dictionary.getAttributeOrRelationAnnotation(
                tableClass,
                MetricAggregation.class,
                fieldName);

        Meta meta = dictionary.getAttributeOrRelationAnnotation(
                tableClass,
                Meta.class,
                fieldName);

        try {
            this.metricFunction = metric.function().newInstance();
            metricFunction.setName(getId() + "[" + metricFunction.getName() + "]");
            metricFunction.setExpression(String.format(
                    metricFunction.getExpression(),
                    dictionary.getAnnotatedColumnName(tableClass, fieldName)));

            if (meta != null) {
                metricFunction.setLongName(meta.longName());
                metricFunction.setDescription(meta.description());
            }
        } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalArgumentException("Can't initialize function for metric " + getId());
        }
    }

    protected MetricFunction constructMetricFunction(String id,
                                                     String longName,
                                                     String description,
                                                     String expression,
                                                     Set<FunctionArgument> arguments) {
        return new MetricFunction(id, longName, description, expression, arguments);
    }
}
