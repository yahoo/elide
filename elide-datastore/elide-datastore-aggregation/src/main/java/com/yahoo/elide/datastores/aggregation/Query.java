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
import com.yahoo.elide.datastores.aggregation.metric.Aggregation;
import com.yahoo.elide.datastores.aggregation.metric.Metric;

import com.yahoo.elide.datastores.aggregation.schema.Schema;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A {@link Query} is an object representing a query executed by {@link QueryEngine}.
 */
@Data
@Builder
public class Query {
    private final Schema schema;

    @Singular
    private final Map<Metric, Class<? extends Aggregation>> metrics;

    @Singular
    private final Set<Dimension> groupDimensions;

    @Singular
    private final Set<TimeDimension> timeDimensions;

    private final FilterExpression whereFilter;
    private final FilterExpression havingFilter;
    private final Sorting sorting;
    private final Pagination pagination;
    private final RequestScope scope;

    /**
     * Returns all the dimensions regardless of type.
     * @return All the dimensions.
     */
    public Set<Dimension> getDimensions() {
        return Stream.concat(getGroupDimensions().stream(), getTimeDimensions().stream())
                .collect(
                        Collectors.toCollection(LinkedHashSet::new)
                );
    }
}
