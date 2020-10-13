/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql.query;

import com.yahoo.elide.datastores.aggregation.query.ColumnProjection;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.datastores.aggregation.query.QueryVisitor;
import com.yahoo.elide.datastores.aggregation.query.Queryable;
import com.yahoo.elide.datastores.aggregation.query.TimeDimensionProjection;

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
            return visitInnerQueryPlan(plan);
        }
    }

    private Query.QueryBuilder visitInnerQueryPlan(Queryable plan)  {
        return Query.builder()
                .source(clientQuery.getSource())
                .metricProjections(plan.getMetricProjections())
                .dimensionProjections(plan.getDimensionProjections())
                .timeDimensionProjections(plan.getTimeDimensionProjections())
                .whereFilter(clientQuery.getWhereFilter())
                //TODO - we only want the dimensions here for their joins - not the metrics.
                .sorting(clientQuery.getSorting());
    }

    private Query.QueryBuilder visitOuterQueryPlan(Queryable plan)  {
        Query innerQuery = plan.accept(this).build();

        Set<ColumnProjection> resourcedDimensions = clientQuery.getDimensionProjections().stream()
                .map(SQLDimensionProjection.class::cast)
                .map((dim) -> dim.withSource(innerQuery))
                .collect(Collectors.toSet());

        Set<TimeDimensionProjection> resourcedTimeDimensions = clientQuery.getTimeDimensionProjections().stream()
                .map(SQLTimeDimensionProjection.class::cast)
                .map((dim) -> dim.withSource(innerQuery))
                .collect(Collectors.toSet());

        return Query.builder()
                .source(innerQuery)
                .dimensionProjections(resourcedDimensions)
                .timeDimensionProjections(resourcedTimeDimensions)
                .havingFilter(clientQuery.getHavingFilter())
                .sorting(clientQuery.getSorting())
                .pagination(clientQuery.getPagination())
                .scope(clientQuery.getScope());
    }

    private Query.QueryBuilder visitMiddleQueryPlan(Queryable plan)  {
        Query innerQuery = plan.accept(this).build();

        Set<ColumnProjection> resourcedDimensions = clientQuery.getDimensionProjections().stream()
                .map(SQLDimensionProjection.class::cast)
                .map((dim) -> dim.withSource(innerQuery))
                .collect(Collectors.toSet());

        Set<TimeDimensionProjection> resourcedTimeDimensions = clientQuery.getTimeDimensionProjections().stream()
                .map(SQLTimeDimensionProjection.class::cast)
                .map((dim) -> dim.withSource(innerQuery))
                .collect(Collectors.toSet());

        return Query.builder()
                .source(innerQuery)
                .dimensionProjections(resourcedDimensions)
                .timeDimensionProjections(resourcedTimeDimensions);
    }
}
