/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.query;

import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import com.google.common.base.Preconditions;
import com.google.common.collect.Streams;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Default implementation of logic to merge two or more query plans into one.
 */
public class DefaultQueryPlanMerger implements QueryPlanMerger {
    MetaDataStore metaDataStore;

    public DefaultQueryPlanMerger(MetaDataStore metaDataStore) {
        this.metaDataStore = metaDataStore;
    }

    @Override
    public boolean canMerge(QueryPlan a, QueryPlan b) {
        Preconditions.checkNotNull(a);
        Preconditions.checkNotNull(b);

        if (a.nestDepth() != b.nestDepth()) {
            if (a.nestDepth() > b.nestDepth() && !b.canNest(metaDataStore)) {
                return false;
            }

            if (b.nestDepth() > a.nestDepth() && !a.canNest(metaDataStore)) {
                return false;
            }

            a = nestToDepth(a, b.nestDepth());
            b = nestToDepth(b, a.nestDepth());
        }

        boolean result = canMergeMetrics(a, b);
        if (! result) {
            return false;
        }

        result = canMergeFilter(a, b);
        if (! result) {
            return false;
        }

        result = canMergeDimensions(a, b);
        if (! result) {
            return false;
        }

        result = canMergeTimeDimensions(a, b);

        if (!result) {
            return false;
        }

        if (a.isNested()) {
            result = canMerge((QueryPlan) a.getSource(), (QueryPlan) b.getSource());
        }

        return result;
    }

    protected boolean canMergeMetrics(QueryPlan a, QueryPlan b) {
        /*
         * Metrics can always coexist provided they have different aliases (which is enforced by the API).
         */
        return true;
    }

    protected boolean canMergeDimensions(QueryPlan a, QueryPlan b) {
        for (DimensionProjection dimension : a.getDimensionProjections()) {
            DimensionProjection otherDimension = b.getDimensionProjection(dimension.getName());

            if (otherDimension != null && ! Objects.equals(otherDimension.getArguments(), dimension.getArguments())) {
                return false;
            }
        }
        return true;
    }

    protected boolean canMergeTimeDimensions(QueryPlan a, QueryPlan b) {
        for (TimeDimensionProjection dimension : a.getTimeDimensionProjections()) {
            TimeDimensionProjection otherDimension = b.getTimeDimensionProjection(dimension.getName());

            if (otherDimension != null && ! Objects.equals(otherDimension.getArguments(), dimension.getArguments())) {
                return false;
            }
        }
        return true;
    }

    protected boolean canMergeFilter(QueryPlan a, QueryPlan b) {
        if (! Objects.equals(a.getWhereFilter(), b.getWhereFilter())) {
            return false;
        }
        return true;
    }

    protected FilterExpression mergeFilter(QueryPlan a, QueryPlan b) {
        return a.getWhereFilter();
    }

    protected Set<MetricProjection> mergeMetrics(QueryPlan a, QueryPlan b) {
        return Streams.concat(b.getMetricProjections().stream(),
                a.getMetricProjections().stream()).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    protected Set<DimensionProjection> mergeDimension(QueryPlan a, QueryPlan b) {
        return Streams.concat(b.getDimensionProjections().stream(),
                a.getDimensionProjections().stream()).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    protected Set<TimeDimensionProjection> mergeTimeDimension(QueryPlan a, QueryPlan b) {
        return Streams.concat(b.getTimeDimensionProjections().stream(),
                a.getTimeDimensionProjections().stream()).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public QueryPlan merge(QueryPlan a, QueryPlan b) {
        Preconditions.checkNotNull(a);
        Preconditions.checkNotNull(b);

        a = nestToDepth(a, b.nestDepth());
        b = nestToDepth(b, a.nestDepth());

        assert (a.isNested() || a.getSource().equals(b.getSource()));

        Set<MetricProjection> metrics = mergeMetrics(a, b);
        Set<TimeDimensionProjection> timeDimensions = mergeTimeDimension(a, b);
        Set<DimensionProjection> dimensions = mergeDimension(a, b);

        if (!a.isNested()) {
            return QueryPlan.builder()
                    .source(a.getSource())
                    .metricProjections(metrics)
                    .dimensionProjections(dimensions)
                    .whereFilter(mergeFilter(a, b))
                    .timeDimensionProjections(timeDimensions)
                    .build();
        }
        Queryable mergedSource = merge((QueryPlan) a.getSource(), (QueryPlan) b.getSource());
        return QueryPlan.builder()
                .source(mergedSource)
                .metricProjections(metrics)
                .dimensionProjections(dimensions)
                .timeDimensionProjections(timeDimensions)
                .build();
    }

    private QueryPlan nestToDepth(QueryPlan plan, int depth) {
        while (plan.nestDepth() < depth) {
            plan = plan.nest(metaDataStore);
        }
        return plan;
    }
}
