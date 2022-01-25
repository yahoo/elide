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
            return false;
        }

        if (! Objects.equals(a.getWhereFilter(), b.getWhereFilter())) {
            return false;
        }

        /*
         * Metrics can always coexist provided they have different aliases (which is enforced by the API).
         */

        for (DimensionProjection dimension : a.getDimensionProjections()) {
            DimensionProjection otherDimension = b.getDimensionProjection(dimension.getName());

            if (otherDimension != null && ! Objects.equals(otherDimension.getArguments(), a.getArguments())) {
                return false;
            }
        }

        for (TimeDimensionProjection dimension : a.getTimeDimensionProjections()) {
            TimeDimensionProjection otherDimension = b.getTimeDimensionProjection(dimension.getName());

            if (otherDimension != null && ! Objects.equals(otherDimension.getArguments(), a.getArguments())) {
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

        while (a.nestDepth() > b.nestDepth()) {
            //TODO - update the reference table on each call to nest.
            //Needed for nesting depth > 2
            b = b.nest(metaDataStore);
        }

        while (b.nestDepth() > a.nestDepth()) {
            //TODO - update the reference table on each call to nest.
            //Needed for nesting depth > 2
            a = a.nest(metaDataStore);
        }

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
                    .whereFilter()
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
}
