/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata.models;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.datastores.aggregation.metadata.metric.AggregatedField;
import com.yahoo.elide.datastores.aggregation.metadata.metric.MetricFunctionInvocation;
import com.yahoo.elide.request.Argument;

import lombok.Data;
import lombok.ToString;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
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
@ToString
public abstract class MetricFunction {
    @Id
    public abstract String getName();

    public abstract String getLongName();

    public abstract String getDescription();

    @OneToMany
    public abstract Set<FunctionArgument> getArguments();

    @Transient
    protected Function<String, String> getArgumentNameMapper() {
        return Function.identity();
    }

    @Transient
    protected abstract MetricFunctionInvocation invoke(Map<String, Argument> arguments,
                                                    AggregatedField field,
                                                    String alias);

    @Transient
    public final MetricFunctionInvocation invoke(Set<Argument> arguments, AggregatedField field, String alias) {
        return invoke(
                arguments.stream()
                        .collect(Collectors.toMap(
                                arg -> getArgumentNameMapper().apply(arg.getName()),
                                Function.identity())),
                field,
                alias);
    }
}
