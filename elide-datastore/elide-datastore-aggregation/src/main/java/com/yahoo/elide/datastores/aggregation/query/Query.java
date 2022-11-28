/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.query;

import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.request.Argument;
import com.yahoo.elide.core.request.Sorting;
import com.yahoo.elide.datastores.aggregation.QueryEngine;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A {@link Query} is an object representing a query executed by {@link QueryEngine}.
 */
@Value
@Builder
public class Query implements Queryable {

    @NonNull
    private Queryable source;

    @Singular
    @NonNull
    private List<MetricProjection> metricProjections;

    @Singular
    @NonNull
    private List<DimensionProjection> dimensionProjections;

    @Singular
    @NonNull
    private List<TimeDimensionProjection> timeDimensionProjections;

    private FilterExpression whereFilter;
    private FilterExpression havingFilter;
    private Sorting sorting;
    private ImmutablePagination pagination;

    @Builder.Default
    private Map<String, Argument> arguments = new HashMap<>();

    @EqualsAndHashCode.Exclude
    private RequestScope scope;

    /**
     * Whether to bypass the {@link QueryEngine} cache for this query.
     */
    @EqualsAndHashCode.Exclude
    private boolean bypassingCache;

    @Override
    public <T> T accept(QueryVisitor<T> visitor) {
        return visitor.visitQuery(this);
    }

    /**
     * Returns all the dimensions regardless of type.
     * @return All the dimensions.
     */
    public List<ColumnProjection> getAllDimensionProjections() {
        return Stream.concat(getDimensionProjections().stream(), getTimeDimensionProjections().stream())
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public static class QueryBuilder {

        public QueryBuilder column(ColumnProjection column) {
            if (column instanceof MetricProjection) {
                metricProjection((MetricProjection) column);
            } else if (column instanceof DimensionProjection) {
                dimensionProjection((DimensionProjection) column);
            } else {
                timeDimensionProjection((TimeDimensionProjection) column);
            }

            return this;
        }

        /**
         * Initializes the builder from another query (copy).
         * @param query What to copy.
         * @return A new query builder.
         */
        public QueryBuilder query(Query query) {
            this.source(query.getSource());
            this.metricProjections(query.getMetricProjections());
            this.dimensionProjections(query.getDimensionProjections());
            this.timeDimensionProjections(query.getTimeDimensionProjections());
            this.arguments(query.getArguments());
            this.sorting(query.getSorting());
            this.pagination(query.getPagination());
            this.whereFilter(query.getWhereFilter());
            this.havingFilter(query.getHavingFilter());
            this.bypassingCache(query.isBypassingCache());
            this.scope(query.getScope());

            return this;
        }
    }
}
