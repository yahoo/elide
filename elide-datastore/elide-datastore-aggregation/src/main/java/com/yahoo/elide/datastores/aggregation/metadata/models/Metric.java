/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata.models;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.datastores.aggregation.AggregationDictionary;
import com.yahoo.elide.datastores.aggregation.metadata.enums.Format;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;

/**
 * Special column for AnalyticView which supports aggregation.
 */
@EqualsAndHashCode(callSuper = true)
@Include(type = "metric")
@Entity
@Data
public class Metric extends Column {
    private Format defaultFormat;

    @ManyToOne
    @ToString.Exclude
    private MetricFunction metricFunction;

    public Metric(Class<?> tableClass, String fieldName, AggregationDictionary dictionary) {
        super(tableClass, fieldName, dictionary);

        com.yahoo.elide.datastores.aggregation.annotation.Metric metric = dictionary.getAttributeOrRelationAnnotation(
                tableClass,
                com.yahoo.elide.datastores.aggregation.annotation.Metric.class,
                fieldName);

        try {
            this.metricFunction = metric.function().newInstance();
            metricFunction.setName(getId() + "[" + metricFunction.getName() + "]");
            metricFunction.setExpression(String.format(
                    metricFunction.getExpression(),
                    dictionary.getColumnName(tableClass, fieldName)));
        } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalArgumentException("Can't initialize function for metric " + getId());
        }
    }
}
