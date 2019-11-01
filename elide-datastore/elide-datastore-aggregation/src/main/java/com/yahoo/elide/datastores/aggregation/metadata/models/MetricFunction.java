/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata.models;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.datastores.aggregation.metadata.enums.Aggregation;

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
    private static final HashMap<Aggregation, MetricFunction> AGGREGATION_FUNCTIONS =
            new HashMap<Aggregation, MetricFunction>() {{
                put(Aggregation.SUM, new MetricFunction("sum", "sum", "sum", new HashSet<>()));
                put(Aggregation.MIN, new MetricFunction("min", "min", "min", new HashSet<>()));
                put(Aggregation.MAX, new MetricFunction("max", "max", "max", new HashSet<>()));
            }};

    @Id
    private String name;

    private String longName;

    private String description;

    @ManyToMany()
    @ToString.Exclude
    private Set<FunctionArgument> arguments;

    public static MetricFunction getAggregationFunction(Aggregation aggregation) {
        return AGGREGATION_FUNCTIONS.get(aggregation);
    }
}
