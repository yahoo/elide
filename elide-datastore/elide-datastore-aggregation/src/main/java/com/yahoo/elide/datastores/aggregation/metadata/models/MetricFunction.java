/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata.models;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.request.Argument;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

import java.util.Map;
import java.util.Set;
import javax.persistence.Id;
import javax.persistence.OneToMany;

/**
 * Functions used to compute metrics.
 */
@Include(type = "metricFunction")
@Data
@ToString
@AllArgsConstructor
public class MetricFunction {
    @Id
    private String name;

    private String description;
    
    private String category;

    private String expression;

    @OneToMany
    private Set<FunctionArgument> arguments;

    /**
     * Construct full metric expression using arguments.
     *
     * @param arguments provided arguments
     * @return <code>FUNCTION(field1, field2, ..., arg1, arg2, ...)</code>
     */
    public String constructExpression(Map<String, Argument> arguments) {
        return getExpression();
    }
}
