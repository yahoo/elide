/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.query;

import com.google.common.collect.Streams;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A {@link QueryPlan} is a partial Query bound to a particular Metric.
 */
@Value
@Builder
public class QueryPlan implements Queryable {

    @NonNull
    private Queryable source;

    @Singular
    @NonNull
    private Set<MetricProjection> metricProjections;

    @Singular
    @NonNull
    private Set<ColumnProjection> dimensionProjections;

    @Singular
    @NonNull
    private Set<TimeDimensionProjection> timeDimensionProjections;

    /**
     * Merges two query plans together.  The order of merged metrics and dimensions is preserved such that
     * the current plan metrics and dimensions come after the requested plan metrics and dimensions.
     * @param other The other query to merge.
     * @return A new merged query plan.
     */
    public QueryPlan merge(QueryPlan other) {
        QueryPlan self = this;

        if (other == null) {
            return this;
        }

        while (other.nestDepth() > self.nestDepth()) {
            self = self.nest();
        }

        while (self.nestDepth() > other.nestDepth()) {
            other = other.nest();
        }

        assert (self.isNested() || getSource().equals(other.getSource()));

        Set<MetricProjection> metrics = Streams.concat(other.metricProjections.stream(),
                self.metricProjections.stream()).collect(Collectors.toCollection(LinkedHashSet::new));

        Set<TimeDimensionProjection> timeDimensions = Streams.concat(other.timeDimensionProjections.stream(),
                self.timeDimensionProjections.stream()).collect(Collectors.toCollection(LinkedHashSet::new));

        Set<ColumnProjection> dimensions = Streams.concat(other.dimensionProjections.stream(),
                self.dimensionProjections.stream()).collect(Collectors.toCollection(LinkedHashSet::new));

        if (!self.isNested()) {
            return QueryPlan.builder()
                    .source(self.getSource())
                    .metricProjections(withSource(self.getSource(), metrics))
                    .dimensionProjections(withSource(self.getSource(), dimensions))
                    .timeDimensionProjections(withSource(self.getSource(), timeDimensions))
                    .build();
        } else {
            Queryable mergedSource = ((QueryPlan) self.getSource()).merge((QueryPlan) other.getSource());
            return QueryPlan.builder()
                    .source(mergedSource)
                    .metricProjections(withSource(self.getSource(), metrics))
                    .dimensionProjections(withSource(self.getSource(), dimensions))
                    .timeDimensionProjections(withSource(self.getSource(), timeDimensions))
                    .build();
        }
    }

    public QueryPlan nest() {
        return QueryPlan.builder()
                .source(this)
                .metricProjections(nestColumnProjection(this, metricProjections))
                .dimensionProjections(nestColumnProjection(this, dimensionProjections))
                .timeDimensionProjections(nestColumnProjection(this, timeDimensionProjections))
                .build();
    }

    /**
     * Makes of a copy of a set of columns all with a new source.
     * @param source The new source.
     * @param columns The columns to copy.
     * @param <T> The column projection type.
     * @return An ordered set of the column copies.
     */
    public static <T extends ColumnProjection> Set<T> withSource(Queryable source, Set<T> columns) {
        return (Set<T>) columns.stream()
                .map(column -> column.withSource(source))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Makes of a copy of a set of columns that are being nested in a new parent.  The column expressions are
     * changed to reference the columns by name.
     * @param parentSource The new parent source.
     * @param columns The columns to copy.
     * @param <T> The column projection type.
     * @return An ordered set of the column copies.
     */
    public static <T extends ColumnProjection> Set<T> nestColumnProjection(Queryable parentSource, Set<T> columns) {
        return (Set<T>) columns.stream()
                .map(column -> column.withSourceAndExpression(parentSource, "{{" + column.getSafeAlias() + "}}"))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
