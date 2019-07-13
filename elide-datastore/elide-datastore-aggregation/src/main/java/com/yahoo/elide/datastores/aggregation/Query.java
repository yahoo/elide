/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation;

import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.pagination.Pagination;
import com.yahoo.elide.core.sort.Sorting;
import com.yahoo.elide.datastores.aggregation.dimension.Dimension;
import com.yahoo.elide.datastores.aggregation.dimension.TimeDimension;
import com.yahoo.elide.datastores.aggregation.metric.Metric;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.util.Optional;
import java.util.Set;

/**
 * A {@link Query} is an object representing a query executed by {@link QueryEngine}.
 */
@Data
@Builder
public class Query {
    private final Class<?> entityClass;

    @Singular
    private final Set<Metric> metrics;

    @Singular
    private final Set<Dimension> groupDimensions;

    @Singular
    private final Set<TimeDimension> timeDimensions;

    @Builder.Default
    private final Optional<FilterExpression> whereFilter = Optional.empty();

    @Builder.Default
    private final Optional<FilterExpression> havingFilter = Optional.empty();

    @Builder.Default
    private final Optional<Sorting> sorting = Optional.empty();

    @Builder.Default
    private final Optional<Pagination> pagination = Optional.empty();

    private final RequestScope scope;
}
