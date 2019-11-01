/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata.models;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.datastores.aggregation.schema.metric.Aggregation;
import com.yahoo.elide.datastores.aggregation.schema.metric.Max;
import com.yahoo.elide.datastores.aggregation.schema.metric.Min;
import com.yahoo.elide.datastores.aggregation.schema.metric.Sum;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;

/**
 * Functions used to compute metrics.
 */
@Include(rootLevel = true, type = "metricFunction")
@Entity
@Data
@AllArgsConstructor
@ToString
public class MetricFunction {
    private static final HashMap<Class<? extends Aggregation>, MetricFunction> AGGREGATION_FUNCTIONS =
            new HashMap<Class<? extends Aggregation>, MetricFunction>() {{
                put(Sum.class, new MetricFunction("sum", "sum", "sum", new HashSet<>()));
                put(Min.class, new MetricFunction("min", "min", "min", new HashSet<>()));
                put(Max.class, new MetricFunction("max", "max", "max", new HashSet<>()));
            }};

    @Id
    private String name;

    private String longName;

    private String description;

    @ManyToMany()
    @ToString.Exclude
    private Set<FunctionArgument> arguments;

    public static MetricFunction getAggregationFunction(Class<? extends Aggregation> aggregation) {
        return AGGREGATION_FUNCTIONS.get(aggregation);
    }
}
