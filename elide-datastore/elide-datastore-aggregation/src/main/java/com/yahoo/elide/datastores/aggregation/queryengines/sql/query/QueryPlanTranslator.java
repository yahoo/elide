/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql.query;

import com.yahoo.elide.datastores.aggregation.query.ColumnProjection;
import com.yahoo.elide.datastores.aggregation.query.MetricProjection;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.datastores.aggregation.query.QueryVisitor;
import com.yahoo.elide.datastores.aggregation.query.Queryable;
import com.yahoo.elide.datastores.aggregation.query.TimeDimensionProjection;
import com.google.common.collect.Streams;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Translates a merged query plan and a client query into query that can be executed.
 */
public class QueryPlanTranslator implements QueryVisitor<Query.QueryBuilder> {

    private Query clientQuery;

    //Whether or not this visitor has been invoked yet.
    private boolean invoked = false;

    public QueryPlanTranslator(Query clientQuery) {
        this.clientQuery = clientQuery;
    }

    @Override
    public Query.QueryBuilder visitQuery(Query query) {
        throw new UnsupportedOperationException("Visitor does not visit queries");
    }

    @Override
    public Query.QueryBuilder visitQueryable(Queryable plan) {
        if (plan.isNested()) {
            if (invoked == false) {
                return visitOuterQueryPlan(plan);
            } else {
                invoked = true;
                return visitMiddleQueryPlan(plan);
            }
        } else {
            if (invoked == true) {
                return visitInnerQueryPlan(plan);
            } else {
                invoked = true;
                return visitUnnestedQueryPlan(plan);
            }
        }
    }

    private Query.QueryBuilder visitInnerQueryPlan(Queryable plan)  {

        Set<ColumnProjection> dimensions = Streams.concat(plan.getDimensionProjections().stream(),
                clientQuery.getDimensionProjections().stream())
                .map(SQLDimensionProjection.class::cast)
                .map(dim -> dim.withSource(clientQuery.getSource()))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Set<TimeDimensionProjection> timeDimensions = Streams.concat(plan.getTimeDimensionProjections().stream(),
                clientQuery.getTimeDimensionProjections().stream())
                .map(SQLTimeDimensionProjection.class::cast)
                .map(dim -> dim.withSource(clientQuery.getSource()))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        return Query.builder()
                .source(clientQuery.getSource())
                .metricProjections(plan.getMetricProjections())
                .dimensionProjections(dimensions)
                .timeDimensionProjections(timeDimensions)
                .whereFilter(clientQuery.getWhereFilter())
                //TODO - we only want the sorting dimensions here for their joins - not the metrics.
                .sorting(clientQuery.getSorting());
    }

    private Query.QueryBuilder visitOuterQueryPlan(Queryable plan)  {
        Query innerQuery = plan.getSource().accept(this).build();

        Set<ColumnProjection> dimensions = clientQuery.getDimensionProjections().stream()
                .map(SQLDimensionProjection.class::cast)
                .map((dim) -> dim.withSource(innerQuery))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Set<TimeDimensionProjection> timeDimensions = clientQuery.getTimeDimensionProjections().stream()
                .map(SQLTimeDimensionProjection.class::cast)
                .map((dim) -> dim.withSource(innerQuery))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Set<MetricProjection> metrics = plan.getMetricProjections()
                .stream()
                .map(SQLMetricProjection.class::cast)
                .map((metric) -> metric.withSource(innerQuery))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        //TODO - whenever there is a nested query, we need to create an inner query where we project out all
        //of the where, having, and sort columns - performing the filters and sorts.   This inner query needs
        //to be wrapped in an outermost query that projects just the final client requested fields.
        return Query.builder()
                .source(innerQuery)
                .metricProjections(metrics)
                .dimensionProjections(dimensions)
                .timeDimensionProjections(timeDimensions)
                .havingFilter(clientQuery.getHavingFilter())
                .sorting(clientQuery.getSorting())
                .pagination(clientQuery.getPagination())
                .scope(clientQuery.getScope());
    }

    private Query.QueryBuilder visitMiddleQueryPlan(Queryable plan)  {
        Query innerQuery = plan.getSource().accept(this).build();

        Set<ColumnProjection> resourcedDimensions = clientQuery.getDimensionProjections().stream()
                .map(SQLDimensionProjection.class::cast)
                .map((dim) -> dim.withSource(innerQuery))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Set<TimeDimensionProjection> resourcedTimeDimensions = clientQuery.getTimeDimensionProjections().stream()
                .map(SQLTimeDimensionProjection.class::cast)
                .map((dim) -> dim.withSource(innerQuery))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Set<MetricProjection> metrics = plan.getMetricProjections()
                .stream()
                .map(SQLMetricProjection.class::cast)
                .map((metric) -> metric.withSource(innerQuery))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        return Query.builder()
                .source(innerQuery)
                .metricProjections(metrics)
                .dimensionProjections(resourcedDimensions)
                .timeDimensionProjections(resourcedTimeDimensions);
    }

    private Query.QueryBuilder visitUnnestedQueryPlan(Queryable plan)  {

        Set<ColumnProjection> dimensions = Streams.concat(plan.getDimensionProjections().stream(),
                clientQuery.getDimensionProjections().stream())
                .map(SQLDimensionProjection.class::cast)
                .map((dim) -> dim.withSource(clientQuery.getSource()))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Set<TimeDimensionProjection> timeDimensions = Streams.concat(plan.getTimeDimensionProjections().stream(),
                clientQuery.getTimeDimensionProjections().stream())
                .map(SQLTimeDimensionProjection.class::cast)
                .map((dim) -> dim.withSource(clientQuery.getSource()))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Set<MetricProjection> metrics = plan.getMetricProjections()
                .stream()
                .map(SQLMetricProjection.class::cast)
                .map((metric) -> metric.withSource(clientQuery.getSource()))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        return Query.builder()
                .source(clientQuery.getSource())
                .metricProjections(metrics)
                .dimensionProjections(dimensions)
                .timeDimensionProjections(timeDimensions)
                .havingFilter(clientQuery.getHavingFilter())
                .whereFilter(clientQuery.getWhereFilter())
                .sorting(clientQuery.getSorting())
                .pagination(clientQuery.getPagination())
                .scope(clientQuery.getScope());
    }
}
