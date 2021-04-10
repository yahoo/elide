/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.query;

import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.request.Sorting;
import com.yahoo.elide.datastores.aggregation.QueryEngine;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A {@link Query} is an object representing a query executed by {@link QueryEngine}.
 */
@Value
@Builder
public class Query implements Queryable {

    @NonNull
    private final Queryable source;

    @Singular
    @NonNull
    private final Set<MetricProjection> metricProjections;

    @Singular
    @NonNull
    private final Set<ColumnProjection> dimensionProjections;

    @Singular
    @NonNull
    private final Set<TimeDimensionProjection> timeDimensionProjections;

    private final FilterExpression whereFilter;
    private final FilterExpression havingFilter;
    private final Sorting sorting;
    private final ImmutablePagination pagination;

    @EqualsAndHashCode.Exclude
    private final RequestScope scope;

    /**
     * Whether to bypass the {@link QueryEngine} cache for this query.
     */
    @EqualsAndHashCode.Exclude
    private final boolean bypassingCache;

    @Override
    public <T> T accept(QueryVisitor<T> visitor) {
        return visitor.visitQuery(this);
    }

    /**
     * Returns all the dimensions regardless of type.
     * @return All the dimensions.
     */
    public Set<ColumnProjection> getAllDimensionProjections() {
        return Stream.concat(getDimensionProjections().stream(), getTimeDimensionProjections().stream())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
