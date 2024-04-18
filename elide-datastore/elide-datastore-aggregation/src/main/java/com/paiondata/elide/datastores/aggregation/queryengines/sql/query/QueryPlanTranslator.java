/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.datastores.aggregation.queryengines.sql.query;

import com.paiondata.elide.core.filter.expression.AndFilterExpression;
import com.paiondata.elide.core.filter.expression.FilterExpression;
import com.paiondata.elide.datastores.aggregation.metadata.MetaDataStore;
import com.paiondata.elide.datastores.aggregation.query.ColumnProjection;
import com.paiondata.elide.datastores.aggregation.query.DefaultQueryPlanMerger;
import com.paiondata.elide.datastores.aggregation.query.Query;
import com.paiondata.elide.datastores.aggregation.query.QueryPlan;
import com.paiondata.elide.datastores.aggregation.query.QueryPlanMerger;
import com.paiondata.elide.datastores.aggregation.query.QueryVisitor;
import com.paiondata.elide.datastores.aggregation.query.Queryable;
import com.paiondata.elide.datastores.aggregation.queryengines.sql.expression.ExpressionParser;
import com.paiondata.elide.datastores.aggregation.queryengines.sql.expression.LogicalReference;
import com.paiondata.elide.datastores.aggregation.queryengines.sql.expression.ReferenceExtractor;
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

    private MetaDataStore metaDataStore;
    private QueryPlanMerger merger;

    public QueryPlanTranslator(Query clientQuery, MetaDataStore metaDataStore, QueryPlanMerger merger) {
        this.metaDataStore = metaDataStore;
        this.clientQuery = clientQuery;
        this.merger = merger;
    }

    public QueryPlanTranslator(Query clientQuery, MetaDataStore metaDataStore) {
        this(clientQuery, metaDataStore, new DefaultQueryPlanMerger(metaDataStore));
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
        if (plan.isNested() && !clientQueryPlan.canNest(metaDataStore)) {
            throw new UnsupportedOperationException("Cannot nest one or more dimensions from the client query");
        }

        QueryPlan merged = merger.merge(clientQueryPlan, plan);

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
                .whereFilter(mergeFilters(plan, clientQuery))
                .arguments(clientQuery.getArguments());

        return addHiddenProjections(metaDataStore, builder, clientQuery);
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
                .whereFilter(mergeFilters(plan, clientQuery))
                .sorting(clientQuery.getSorting())
                .pagination(clientQuery.getPagination())
                .scope(clientQuery.getScope())
                .arguments(clientQuery.getArguments());

        return addHiddenProjections(metaDataStore, builder, clientQuery);
    }

    public static Query.QueryBuilder addHiddenProjections(
            MetaDataStore metaDataStore,
            Query.QueryBuilder builder,
            Query query
    ) {

        Set<ColumnProjection> directReferencedColumns = Streams.concat(
                query.getColumnProjections().stream(),
                query.getFilterProjections(query.getWhereFilter(), ColumnProjection.class).stream()
        ).collect(Collectors.toSet());

        ExpressionParser parser = new ExpressionParser(metaDataStore);
        Set<ColumnProjection> indirectReferenceColumns = new HashSet<>();
        directReferencedColumns.forEach(column -> {
            parser.parse(query.getSource(), column).stream()
                    .map(reference -> reference.accept(new ReferenceExtractor<LogicalReference>(
                            LogicalReference.class,
                            metaDataStore,
                            ReferenceExtractor.Mode.SAME_QUERY)))
                    .flatMap(Set::stream)
                    .map(LogicalReference::getColumn)
                    .forEach(indirectReferenceColumns::add);
        });

        Streams.concat(
                directReferencedColumns.stream(),
                indirectReferenceColumns.stream()
        ).forEach(column -> {
            if (query.getColumnProjection(column.getAlias(), column.getArguments()) == null) {
                builder.column(column.withProjected(false));
            }
        });

        return builder;
    }

    public static Query.QueryBuilder addHiddenProjections(MetaDataStore metaDataStore, Query query) {
        Query.QueryBuilder builder = Query.builder()
                        .source(query.getSource())
                        .metricProjections(query.getMetricProjections())
                        .dimensionProjections(query.getDimensionProjections())
                        .timeDimensionProjections(query.getTimeDimensionProjections())
                        .havingFilter(query.getHavingFilter())
                        .whereFilter(query.getWhereFilter())
                        .sorting(query.getSorting())
                        .pagination(query.getPagination())
                        .scope(query.getScope())
                        .arguments(query.getArguments());

        return addHiddenProjections(metaDataStore, builder, query);
    }

    private static FilterExpression mergeFilters(Queryable a, Queryable b) {
        if (a.getWhereFilter() == null && b.getWhereFilter() == null) {
            return null;
        }

        if (a.getWhereFilter() == null) {
            return b.getWhereFilter();
        }

        if (b.getWhereFilter() == null) {
            return a.getWhereFilter();
        }

        return new AndFilterExpression(a.getWhereFilter(), b.getWhereFilter());
    }
}
