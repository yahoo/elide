/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata.models;

import com.yahoo.elide.annotation.Include;

import com.yahoo.elide.datastores.aggregation.query.MetricProjection;
import com.yahoo.elide.datastores.aggregation.query.Query;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

import java.util.Set;
import javax.persistence.Id;
import javax.persistence.OneToMany;

/**
 * Functions used to compute metrics.
 */
@Include(rootLevel = false, type = "metricFunction")
@Data
@ToString
@AllArgsConstructor
public class MetricFunction {
    @Id
    private String name;

    private String description;

    private String expression;

    @OneToMany
    private Set<FunctionArgument> arguments;

    public Query resolve(Query query, MetricProjection metric) {
        return Query.builder()
                .metricProjection(metric)
                .source(query.getSource())
                .dimensionProjections(query.getAllDimensionProjections())
                .timeDimensionProjections(query.getTimeDimensionProjections())
                .whereFilter(query.getWhereFilter())
                .havingFilter(query.getHavingFilter())
                .sorting(query.getSorting())
                .pagination(query.getPagination())
                .scope(query.getScope())
                .build();
    }
}
