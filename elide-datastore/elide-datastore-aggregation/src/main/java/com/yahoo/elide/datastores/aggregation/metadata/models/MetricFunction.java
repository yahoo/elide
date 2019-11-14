/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata.models;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.core.exceptions.InvalidPredicateException;
import com.yahoo.elide.datastores.aggregation.metadata.metric.AggregatedField;
import com.yahoo.elide.datastores.aggregation.metadata.metric.MetricFunctionInvocation;
import com.yahoo.elide.request.Argument;

import lombok.Data;
import lombok.ToString;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;

/**
 * Functions used to compute metrics.
 */
@Include(type = "metricFunction")
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

    private Set<String> getArgumentNames() {
        return getArguments().stream().map(FunctionArgument::getName).collect(Collectors.toSet());
    }

    protected abstract MetricFunctionInvocation invoke(Map<String, Argument> arguments,
                                                    AggregatedField field,
                                                    String alias);

    /**
     * Invoke this metric function with arguments, an aggregated field and projection alias.
     *
     * @param arguments arguments provided in the request
     * @param field field to apply this function
     * @param alias result alias
     * @return an invoked metric function
     */
    public final MetricFunctionInvocation invoke(Set<Argument> arguments, AggregatedField field, String alias) {
        Set<String> requiredArguments = getArgumentNames();
        Set<String> providedArguments = arguments.stream()
                .map(Argument::getName)
                .collect(Collectors.toSet());

        if (!requiredArguments.equals(providedArguments)) {
            throw new InvalidPredicateException(
                    "Provided arguments doesn't match requirement for function " + getName() + ".");
        }

        // map arguments to their actual name
        Map<String, Argument> resolvedArguments = arguments.stream()
                .collect(Collectors.toMap(Argument::getName, Function.identity()));

        return invoke(resolvedArguments, field, alias);
    }
}
