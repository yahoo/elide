/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata.models;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.datastores.aggregation.metadata.metric.MetricFunctionInvocation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Transient;

/**
 * Functions used to compute metrics.
 */
@Include(rootLevel = true, type = "metricFunction")
@Entity
@Data
@AllArgsConstructor
@ToString
public abstract class MetricFunction {
    @Id
    private String name;

    private String longName;

    private String description;

    @OneToMany
    @ToString.Exclude
    private Set<FunctionArgument> arguments;

    @Transient
    public abstract MetricFunctionInvocation invoke(String[] arguments, Metric metric, String alias);
}
