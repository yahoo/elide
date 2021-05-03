/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql.query;

import com.yahoo.elide.datastores.aggregation.query.ColumnProjection;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.datastores.aggregation.query.QueryPlan;
import com.yahoo.elide.datastores.aggregation.query.QueryVisitor;
import com.yahoo.elide.datastores.aggregation.query.Queryable;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.expression.LogicalReference;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.expression.LogicalReferenceExtractor;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLReferenceTable;

import com.google.common.collect.Streams;

import java.util.HashSet;
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

    /**
     * Translate a query plan into a query.
     * @param plan the plan to translate.
     * @return A runnable query incorporating elements from the client's original query.
     */
    public Query translate(QueryPlan plan) {

        //Convert the client query into a dimension-only plan that can be nested.
        QueryPlan clientQueryPlan = QueryPlan.builder()
                .source(clientQuery.getSource())
                .timeDimensionProjections(clientQuery.getTimeDimensionProjections())
                .dimensionProjections(clientQuery.getDimensionProjections())
                .build();

        //Merge it with the metric plan.
        if (plan.isNested() && !clientQueryPlan.canNest(lookupTable)) {
            throw new UnsupportedOperationException("Cannot nest one or more dimensions from the client query");
        }

        QueryPlan merged = clientQueryPlan.merge(plan, lookupTable);

        //Decorate the result with filters, sorting, and pagination.
        return merged.accept(this).build();
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
            }
            return visitUnnestedQueryPlan(plan);
        }
        if (plan.isNested()) {
            return visitMiddleQueryPlan(plan);
        }
        return visitInnerQueryPlan(plan);
    }

    private Query.QueryBuilder visitInnerQueryPlan(Queryable plan)  {
        Query.QueryBuilder builder = Query.builder()
                .source(clientQuery.getSource())
                .metricProjections(plan.getMetricProjections())
                .dimensionProjections(plan.getDimensionProjections())
                .timeDimensionProjections(plan.getTimeDimensionProjections())
                .whereFilter(clientQuery.getWhereFilter())
                .arguments(clientQuery.getArguments());

        return addHiddenProjections(lookupTable, builder, clientQuery);
    }

    private Query.QueryBuilder visitOuterQueryPlan(Queryable plan)  {
        Query innerQuery = plan.getSource().accept(this).build();

        //This assumes SORT & HAVING clauses must reference projection columns (verified in QueryValidator).
        //Otherwise, the SORT & HAVING may reference joins that have not taken place in the inner query.
        return Query.builder()
                .source(innerQuery)
                .metricProjections(plan.getMetricProjections())
                .dimensionProjections(plan.getDimensionProjections())
                .timeDimensionProjections(plan.getTimeDimensionProjections())
                .havingFilter(clientQuery.getHavingFilter())
                .sorting(clientQuery.getSorting())
                .pagination(clientQuery.getPagination())
                .scope(clientQuery.getScope())
                .arguments(clientQuery.getArguments());
    }

    private Query.QueryBuilder visitMiddleQueryPlan(Queryable plan)  {
        Query innerQuery = plan.getSource().accept(this).build();

        //TODO - Add tests for middle.
        return Query.builder()
                .source(innerQuery)
                .metricProjections(plan.getMetricProjections())
                .dimensionProjections(plan.getDimensionProjections())
                .timeDimensionProjections(plan.getTimeDimensionProjections());
    }

    private Query.QueryBuilder visitUnnestedQueryPlan(Queryable plan)  {

        Query.QueryBuilder builder = Query.builder()
                .source(clientQuery.getSource())
                .metricProjections(plan.getMetricProjections())
                .dimensionProjections(plan.getDimensionProjections())
                .timeDimensionProjections(plan.getTimeDimensionProjections())
                .havingFilter(clientQuery.getHavingFilter())
                .whereFilter(clientQuery.getWhereFilter())
                .sorting(clientQuery.getSorting())
                .pagination(clientQuery.getPagination())
                .scope(clientQuery.getScope())
                .arguments(clientQuery.getArguments());

        return addHiddenProjections(lookupTable, builder, clientQuery);
    }

    public static Query.QueryBuilder addHiddenProjections(
            SQLReferenceTable lookupTable,
            Query.QueryBuilder builder,
            Query query
    ) {

        Set<ColumnProjection> directReferencedColumns = Streams.concat(
                query.getColumnProjections().stream(),
                query.getFilterProjections(query.getWhereFilter(), ColumnProjection.class).stream()
        ).collect(Collectors.toSet());

        Set<ColumnProjection> indirectReferenceColumns = new HashSet<>();
        directReferencedColumns.forEach(column -> {
            lookupTable.getReferenceTree(query.getSource(), column.getName()).stream()
                    .map(reference -> reference.accept(new LogicalReferenceExtractor(lookupTable.getMetaDataStore())))
                    .flatMap(Set::stream)
                    .map(LogicalReference::getColumn)
                    .forEach(indirectReferenceColumns::add);
        });

        Streams.concat(
                directReferencedColumns.stream(),
                indirectReferenceColumns.stream()
        ).forEach(column -> {
            if (query.getColumnProjection(column.getName(), column.getArguments()) == null) {
                builder.column(column.withProjected(false));
            }
        });

        return builder;
    }
}
