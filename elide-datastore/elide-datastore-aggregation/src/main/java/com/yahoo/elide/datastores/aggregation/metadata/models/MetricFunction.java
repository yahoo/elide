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

import java.util.AbstractMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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

    private Set<String> getArgumentNames() {
        return getArguments().stream().map(FunctionArgument::getName).collect(Collectors.toSet());
    }

    /**
     * Metric function can have an argument name converter to convert an argument alias to actually argument name.
     *
     * @return a string-to-string mapping function
     */
    @Transient
    protected Function<String, List<String>> getArgumentNameMapper() {
        return Collections::singletonList;
    }

    @Transient
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
    @Transient
    public final MetricFunctionInvocation invoke(Set<Argument> arguments, AggregatedField field, String alias) {
        Set<String> requiredArguments = getArgumentNames();

        Map<String, Argument> providedArguments = arguments.stream()
                .filter(arg -> requiredArguments.contains(arg.getName()))
                .collect(Collectors.toMap(Argument::getName, Function.identity()));

        // map arguments to their actual name
        Map<String, Argument> resolvedArguments = requiredArguments.stream()
                .map(argName -> {
                    if (!providedArguments.containsKey(argName)) {
                        throw new InvalidPredicateException("Argument Not Found " + getName() + "." + argName + ".");
                    }
                    Argument arg = providedArguments.get(argName);
                    List<String> actualNames = getArgumentNameMapper().apply(argName);
                    return actualNames.stream().map(name -> new AbstractMap.SimpleEntry<>(name, arg));
                })
                .reduce(Stream.empty(), Stream::concat)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        return invoke(resolvedArguments, field, alias);
    }
}
