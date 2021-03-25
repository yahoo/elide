/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql.query;

import static com.yahoo.elide.datastores.aggregation.queryengines.sql.query.SQLColumnProjection.innerQueryProjections;
import static com.yahoo.elide.datastores.aggregation.queryengines.sql.query.SQLColumnProjection.outerQueryProjections;
import com.yahoo.elide.datastores.aggregation.query.ColumnProjection;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.datastores.aggregation.query.QueryVisitor;
import com.yahoo.elide.datastores.aggregation.query.Queryable;
import com.yahoo.elide.datastores.aggregation.query.TimeDimensionProjection;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLReferenceTable;
import com.google.common.collect.Streams;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Translates a merged query plan and a client query into query that can be executed.
 *
 * Column projection expressions originating from the query plan are left untouched.
 * Column projection expressions originating from the client query are nested for outer queries.
 */
public class QueryPlanTranslator implements QueryVisitor<Query.QueryBuilder> {

    private Query clientQuery;

    //Whether or not this visitor has been invoked yet.
    private boolean invoked = false;

    private SQLReferenceTable lookupTable;

    public QueryPlanTranslator(Query clientQuery, SQLReferenceTable lookupTable) {
        this.lookupTable = lookupTable;
        this.clientQuery = clientQuery;
    }

    @Override
    public Query.QueryBuilder visitQuery(Query query) {
        throw new UnsupportedOperationException("Visitor does not visit queries");
    }

    @Override
    public Query.QueryBuilder visitQueryable(Queryable plan) {
        if (! invoked) {
            invoked = true;
            if (plan.isNested()) {
                return visitOuterQueryPlan(plan);
            } else {
                return visitUnnestedQueryPlan(plan);
            }
        } else {
            if (plan.isNested()) {
                return visitMiddleQueryPlan(plan);
            } else {
                return visitInnerQueryPlan(plan);
            }
        }
    }

    private Query.QueryBuilder visitInnerQueryPlan(Queryable plan)  {

        Set<ColumnProjection> dimensions = Streams.concat(plan.getDimensionProjections().stream(),
                innerQueryProjections(clientQuery, clientQuery.getDimensionProjections(), lookupTable).stream())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Set<TimeDimensionProjection> timeDimensions = Streams.concat(plan.getTimeDimensionProjections().stream(),
                innerQueryProjections(clientQuery, clientQuery.getTimeDimensionProjections(), lookupTable).stream())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        return Query.builder()
                .source(clientQuery.getSource())
                .metricProjections(plan.getMetricProjections())
                .dimensionProjections(dimensions)
                .timeDimensionProjections(timeDimensions)
                .whereFilter(clientQuery.getWhereFilter());
    }

    private Query.QueryBuilder visitOuterQueryPlan(Queryable plan)  {
        Query innerQuery = plan.getSource().accept(this).build();

        return Query.builder()
                .source(innerQuery)
                .metricProjections(plan.getMetricProjections())
                .dimensionProjections(outerQueryProjections(clientQuery, clientQuery.getDimensionProjections(),
                        lookupTable))
                .timeDimensionProjections(outerQueryProjections(clientQuery, clientQuery.getTimeDimensionProjections(),
                        lookupTable))
                .havingFilter(clientQuery.getHavingFilter())
                .sorting(clientQuery.getSorting())
                .pagination(clientQuery.getPagination())
                .scope(clientQuery.getScope());
    }

    private Query.QueryBuilder visitMiddleQueryPlan(Queryable plan)  {
        Query innerQuery = plan.getSource().accept(this).build();


        return Query.builder()
                .source(innerQuery)
                .metricProjections(plan.getMetricProjections())
                .dimensionProjections(innerQueryProjections(clientQuery, clientQuery.getDimensionProjections(),
                        lookupTable))
                .timeDimensionProjections(innerQueryProjections(clientQuery, clientQuery.getTimeDimensionProjections(),
                        lookupTable));
    }

    private Query.QueryBuilder visitUnnestedQueryPlan(Queryable plan)  {

        Set<ColumnProjection> dimensions = Streams.concat(plan.getDimensionProjections().stream(),
            clientQuery.getDimensionProjections().stream())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Set<TimeDimensionProjection> timeDimensions = Streams.concat(plan.getTimeDimensionProjections().stream(),
            clientQuery.getTimeDimensionProjections().stream())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        return Query.builder()
                .source(clientQuery.getSource())
                .metricProjections(plan.getMetricProjections())
                .dimensionProjections(dimensions)
                .timeDimensionProjections(timeDimensions)
                .havingFilter(clientQuery.getHavingFilter())
                .whereFilter(clientQuery.getWhereFilter())
                .sorting(clientQuery.getSorting())
                .pagination(clientQuery.getPagination())
                .scope(clientQuery.getScope());
    }
}
