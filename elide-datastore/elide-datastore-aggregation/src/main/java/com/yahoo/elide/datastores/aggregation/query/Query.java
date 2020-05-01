/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.query;

import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.datastores.aggregation.QueryEngine;
import com.yahoo.elide.datastores.aggregation.metadata.models.Table;
import com.yahoo.elide.request.Pagination;
import com.yahoo.elide.request.Sorting;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A {@link Query} is an object representing a query executed by {@link QueryEngine}.
 */
@Value
@Builder
public class Query {
    Table table;

    @Singular
    List<MetricProjection> metrics;

    @Singular
    Set<ColumnProjection> groupByDimensions;

    @Singular
    Set<TimeDimensionProjection> timeDimensions;

    FilterExpression whereFilter;
    FilterExpression havingFilter;
    Sorting sorting;
    Pagination pagination;
    RequestScope scope;

    /**
     * Returns all the dimensions regardless of type.
     * @return All the dimensions.
     */
    public Set<ColumnProjection> getDimensions() {
        return Stream.concat(getGroupByDimensions().stream(), getTimeDimensions().stream())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
