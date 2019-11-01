/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata.models;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.datastores.aggregation.AggregationDictionary;
import com.yahoo.elide.datastores.aggregation.annotation.MetricAggregation;
import com.yahoo.elide.datastores.aggregation.annotation.MetricComputation;
import com.yahoo.elide.datastores.aggregation.metadata.enums.Aggregation;
import com.yahoo.elide.datastores.aggregation.metadata.enums.Format;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.persistence.Entity;
import javax.persistence.ManyToMany;

/**
 * Special column for AnalyticView which supports aggregation
 */
@EqualsAndHashCode(callSuper = true)
@Include(rootLevel = true, type = "metric")
@Entity
@Data
public class Metric extends Column {
    private Format defaultFormat;

    @ManyToMany
    @ToString.Exclude
    private List<MetricFunction> supportedFunctions;

    public Metric(Class<?> tableClass, String fieldName, AggregationDictionary dictionary) {
        super(tableClass, fieldName, dictionary);

        if (dictionary.attributeOrRelationAnnotationExists(tableClass, fieldName, MetricAggregation.class)) {
            MetricAggregation metricAggregation = dictionary.getAttributeOrRelationAnnotation(
                    tableClass,
                    MetricAggregation.class,
                    fieldName);

            Aggregation[] aggregations = metricAggregation.aggregations();

            this.supportedFunctions = Arrays.stream(aggregations)
                    .map(MetricFunction::getAggregationFunction)
                    .collect(Collectors.toList());
        } else if (dictionary.attributeOrRelationAnnotationExists(tableClass, fieldName, MetricComputation.class)) {
            MetricComputation metricComputation = dictionary.getAttributeOrRelationAnnotation(
                    tableClass,
                    MetricComputation.class,
                    fieldName);

            // TODO: parse MetricComputation expression
            this.supportedFunctions = new ArrayList<>();
        } else {
            throw new IllegalArgumentException(getId() + " is not a metric field");
        }
    }
}
