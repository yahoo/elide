/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.query;

import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLReferenceTable;
import com.google.common.collect.Streams;
import org.apache.commons.lang3.tuple.Pair;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;

import java.util.LinkedHashSet;
import java.util.List;
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
    private List<MetricProjection> metricProjections;

    @Singular
    @NonNull
    private List<DimensionProjection> dimensionProjections;

    @Singular
    @NonNull
    private List<TimeDimensionProjection> timeDimensionProjections;

    /**
     * Merges two query plans together.  The order of merged metrics and dimensions is preserved such that
     * the current plan metrics and dimensions come after the requested plan metrics and dimensions.
     * @param other The other query to merge.
     * @return A new merged query plan.
     */
    public QueryPlan merge(QueryPlan other, SQLReferenceTable lookupTable) {
        QueryPlan self = this;

        if (other == null) {
            return this;
        }

        while (other.nestDepth() > self.nestDepth()) {
            //TODO - update the reference table on each call to nest.
            //Needed for nesting depth > 2
            self = self.nest(lookupTable);
        }

        while (self.nestDepth() > other.nestDepth()) {
            //TODO - update the reference table on each call to nest.
            //Needed for nesting depth > 2
            other = other.nest(lookupTable);
        }

        assert (self.isNested() || getSource().equals(other.getSource()));

        Set<MetricProjection> metrics = Streams.concat(other.metricProjections.stream(),
                self.metricProjections.stream()).collect(Collectors.toCollection(LinkedHashSet::new));

        Set<TimeDimensionProjection> timeDimensions = Streams.concat(other.timeDimensionProjections.stream(),
                self.timeDimensionProjections.stream()).collect(Collectors.toCollection(LinkedHashSet::new));

        Set<DimensionProjection> dimensions = Streams.concat(other.dimensionProjections.stream(),
                self.dimensionProjections.stream()).collect(Collectors.toCollection(LinkedHashSet::new));

        if (!self.isNested()) {
            return QueryPlan.builder()
                    .source(self.getSource())
                    .metricProjections(metrics)
                    .dimensionProjections(dimensions)
                    .timeDimensionProjections(timeDimensions)
                    .build();
        }
        Queryable mergedSource = ((QueryPlan) self.getSource()).merge((QueryPlan) other.getSource(), lookupTable);
        return QueryPlan.builder()
                .source(mergedSource)
                .metricProjections(metrics)
                .dimensionProjections(dimensions)
                .timeDimensionProjections(timeDimensions)
                .build();
    }

    /**
     * Tests whether or not this plan can be nested.
     * @param lookupTable Used for resolving expression templates.
     * @return True if the projection can be nested.  False otherwise.
     */
    public boolean canNest(SQLReferenceTable lookupTable) {
        return getColumnProjections().stream().allMatch(projection -> projection.canNest(source, lookupTable));
    }

    /**
     * Breaks a flat query into a nested query.  There are multiple approaches for how to do this, but
     * this kind of nesting requires aggregation to happen both in the inner and outer queries.  This allows
     * query plans with two-pass aggregations to be merged with simpler one-pass aggregation plans.
     *
     * The nesting performed here attempts to perform all joins in the inner query.
     *
     * @param lookupTable Needed for answering questions about templated SQL column definitions.
     * @return A nested query plan.
     */
    public QueryPlan nest(SQLReferenceTable lookupTable) {
        if (!canNest(lookupTable)) {
            throw new UnsupportedOperationException("Cannot nest this query plan");
        }

        Set<Pair<ColumnProjection, Set<ColumnProjection>>> nestedMetrics = metricProjections.stream()
                .map(projection -> projection.nest(source, lookupTable, false))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Set<Pair<ColumnProjection, Set<ColumnProjection>>> nestedDimensions = dimensionProjections.stream()
                .map(projection -> projection.nest(source, lookupTable, false))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Set<Pair<ColumnProjection, Set<ColumnProjection>>> nestedTimeDimensions = timeDimensionProjections.stream()
                .map(projection -> projection.nest(source, lookupTable, false))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        QueryPlan inner = QueryPlan.builder()
                .source(this.getSource())
                .metricProjections(nestedMetrics.stream()
                        .map(Pair::getRight)
                        .flatMap(Set::stream)
                        .map(MetricProjection.class::cast)
                        .collect(Collectors.toCollection(LinkedHashSet::new)))
                .dimensionProjections(nestedDimensions.stream()
                        .map(Pair::getRight)
                        .flatMap(Set::stream)
                        .map(DimensionProjection.class::cast)
                        .collect(Collectors.toCollection(LinkedHashSet::new)))
                .timeDimensionProjections(nestedTimeDimensions.stream()
                        .map(Pair::getRight)
                        .flatMap(Set::stream)
                        .map(TimeDimensionProjection.class::cast)
                        .collect(Collectors.toCollection(LinkedHashSet::new)))
                .build();

        return QueryPlan.builder()
                .source(inner)
                .metricProjections(nestedMetrics.stream()
                        .map(Pair::getLeft)
                        .map(MetricProjection.class::cast)
                        .collect(Collectors.toCollection(LinkedHashSet::new)))
                .dimensionProjections(nestedDimensions.stream()
                        .map(Pair::getLeft)
                        .map(DimensionProjection.class::cast)
                        .collect(Collectors.toCollection(LinkedHashSet::new)))
                .timeDimensionProjections(nestedTimeDimensions.stream()
                        .map(Pair::getLeft)
                        .map(TimeDimensionProjection.class::cast)
                        .collect(Collectors.toCollection(LinkedHashSet::new)))
                .build();
    }
}
