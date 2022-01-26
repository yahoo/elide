/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.query;

import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;

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
        if (a == null || b == null) {
            return true;
        }

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

        if (! Objects.equals(a.getWhereFilter(), b.getWhereFilter())) {
            return false;
        }

        /*
         * Metrics can always coexist provided they have different aliases (which is enforced by the API).
         */

        for (DimensionProjection dimension : a.getDimensionProjections()) {
            DimensionProjection otherDimension = b.getDimensionProjection(dimension.getName());

            if (otherDimension != null && ! Objects.equals(otherDimension.getArguments(), dimension.getArguments())) {
                return false;
            }
        }

        for (TimeDimensionProjection dimension : a.getTimeDimensionProjections()) {
            TimeDimensionProjection otherDimension = b.getTimeDimensionProjection(dimension.getName());

            if (otherDimension != null && ! Objects.equals(otherDimension.getArguments(), dimension.getArguments())) {
                return false;
            }
        }

        return true;
    }

    @Override
    public QueryPlan merge(QueryPlan a, QueryPlan b) {

        if (b == null) {
            return a;
        }

        if (a == null) {
            return b;
        }

        a = nestToDepth(a, b.nestDepth());
        b = nestToDepth(b, a.nestDepth());

        assert (a.isNested() || a.getSource().equals(b.getSource()));

        Set<MetricProjection> metrics = Streams.concat(b.getMetricProjections().stream(),
                a.getMetricProjections().stream()).collect(Collectors.toCollection(LinkedHashSet::new));

        Set<TimeDimensionProjection> timeDimensions = Streams.concat(b.getTimeDimensionProjections().stream(),
                a.getTimeDimensionProjections().stream()).collect(Collectors.toCollection(LinkedHashSet::new));

        Set<DimensionProjection> dimensions = Streams.concat(b.getDimensionProjections().stream(),
                a.getDimensionProjections().stream()).collect(Collectors.toCollection(LinkedHashSet::new));

        if (!a.isNested()) {
            return QueryPlan.builder()
                    .source(a.getSource())
                    .metricProjections(metrics)
                    .dimensionProjections(dimensions)
                    .whereFilter(a.getWhereFilter())
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
