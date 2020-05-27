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
import com.yahoo.elide.request.Sorting;
import lombok.Builder;
import lombok.NonNull;
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
    @NonNull
    private Table table;

    @Singular
    private List<MetricProjection> metrics;

    @Singular
    private Set<ColumnProjection> groupByDimensions;

    @Singular
    private Set<TimeDimensionProjection> timeDimensions;

    private FilterExpression whereFilter;
    private FilterExpression havingFilter;
    private Sorting sorting;
    private ImmutablePagination pagination;
    private RequestScope scope;

    /**
     * Whether to bypass the {@link QueryEngine} cache for this query.
     */
    private boolean bypassingCache;

    /**
     * Returns all the dimensions regardless of type.
     * @return All the dimensions.
     */
    public Set<ColumnProjection> getDimensions() {
        return Stream.concat(getGroupByDimensions().stream(), getTimeDimensions().stream())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
