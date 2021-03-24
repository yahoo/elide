/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql.query;

import static com.yahoo.elide.datastores.aggregation.queryengines.sql.query.SQLColumnProjection.innerQueryProjections;
import static com.yahoo.elide.datastores.aggregation.queryengines.sql.query.SQLColumnProjection.outerQueryProjections;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.expression.PredicateExtractionVisitor;
import com.yahoo.elide.core.filter.predicates.FilterPredicate;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import com.yahoo.elide.datastores.aggregation.query.ColumnProjection;
import com.yahoo.elide.datastores.aggregation.query.MetricProjection;
import com.yahoo.elide.datastores.aggregation.query.Optimizer;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.datastores.aggregation.query.QueryVisitor;
import com.yahoo.elide.datastores.aggregation.query.Queryable;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLReferenceTable;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLTable;

import com.google.common.collect.Sets;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AggregateBeforeJoinOptimizer implements Optimizer {
    private MetaDataStore metaDataStore;

    public AggregateBeforeJoinOptimizer(MetaDataStore metaDataStore) {
        this.metaDataStore = metaDataStore;
    }

    private class OptimizerVisitor implements QueryVisitor<Queryable> {
        private SQLReferenceTable lookupTable;

        public OptimizerVisitor(SQLReferenceTable lookupTable) {
            this.lookupTable = lookupTable;
        }

        @Override
        public Queryable visitQuery(Query query) {
            SubqueryFilterSplitter.SplitFilter splitWhere =
                    SubqueryFilterSplitter.splitFilter(lookupTable, metaDataStore, query.getWhereFilter());

            Query inner = Query.builder()
                    .source(query.getSource().accept(this))
                    .metricProjections(Sets.union(
                            innerQueryProjections(query, query.getMetricProjections(), lookupTable),
                            innerQueryProjections(query, extractHavingMetrics(query), lookupTable))
                    )
                    .dimensionProjections(Sets.union(
                        Sets.union(innerQueryProjections(query, query.getDimensionProjections(), lookupTable),
                        extractInnerQueryJoinProjections(query)),
                        extractInnerQueryJoinProjections(splitWhere.getOuter()))
                    )
                    //TODO join projections may either be time dimensions OR regular dimensions - we
                    //should split accordingly
                    .timeDimensionProjections(innerQueryProjections(query, query.getTimeDimensionProjections(),
                            lookupTable))
                    .whereFilter(splitWhere.getInner())
                    .build();

            return Query.builder()
                    //Outer HAVING filters may reference columns in the inner query that need to be properly nested.
                    .metricProjections(Sets.union(
                            outerQueryProjections(query, query.getMetricProjections(), lookupTable),
                            outerQueryProjections(query, getVirtualMetrics((SQLTable) query.getSource(),
                                    query.getHavingFilter()), lookupTable)))
                    .dimensionProjections(Sets.union(Sets.union(
                            outerQueryProjections(query, query.getDimensionProjections(), lookupTable),

                            //TODO - Do these dimensions also need to be projected in the inner query (assuming they
                            //are not joined? They were never projected in the client query (hence being virtual), but
                            //the inner query will hide them if they are not joins.  We need to differentiate between
                            //virtual columns that require joins (no nesting) and those that don't (require nesting).
                            getVirtualDims((SQLTable) query.getSource(), splitWhere.getOuter())),
                            getVirtualDims((SQLTable) query.getSource(), query.getHavingFilter())))
                    .timeDimensionProjections(outerQueryProjections(query, query.getTimeDimensionProjections(),
                            lookupTable))
                    .whereFilter(splitWhere.getOuter())
                    .havingFilter(query.getHavingFilter())
                    .sorting(query.getSorting())
                    .pagination(query.getPagination())
                    .scope(query.getScope())
                    .bypassingCache(query.isBypassingCache())
                    .source(inner)
                    .build();
        }

        private Set<SQLMetricProjection> getInnerQueryMetrics(Query query) {
            //1.  Inner query metrics requested by client. Nested.
            //2.  Inner query metrics required for having clause but missing in client request.  Nested.
            //3.  Inner query metrics required for sort clause but missing in client request.  Nested.

            //We don't need join columns for metrics (we refused to optimize if the metric requires a join).
            //There is no where clause for metrics.

            return null;
        }

        private Set<SQLMetricProjection> getOuterQueryMetrics(Query query) {
            //1.  Outer query metrics requested by client.  Nested.
            //2.  Outer hidden/virtual query metrics required for having filters.  Nested.
            //3.  Outer hidden/virtual query metrics required for sort clause.  Nested.

            return null;
        }

        private Set<SQLDimensionProjection> getInnerQueryDimensions(Query query) {

            /* Nesting Rules:
                - Metrics: everything inside Aggregation function in inner query.  Post agg in outer query.
                - Dimensions without joins: everything in inner query.  Alias reference in outer query.
                - Dimensions with joins: Physical columns projected in inner query.  Everything else applied post agg.
                - Outer columns are virtual if they only appear in HAVING, WHERE, or SORT.
             */

            return null;
        }

        //We'll need this to work for both dimensions and time dimensions.
        private Set<SQLDimensionProjection> getOuterQueryDimensions(Query query) {
            return null;
        }

        private Set<SQLColumnProjection> extractInnerQueryJoinProjections(Query query) {
            return query.getColumnProjections().stream()
                    .flatMap(column -> {
                        return lookupTable.getResolvedJoinProjections(query.getSource(), column.getName()).stream();
                    })
                    .collect(Collectors.toSet());
        }

        private Set<MetricProjection> extractHavingMetrics(Query query) {
            FilterExpression having = query.getHavingFilter();
            if (having == null) {
                return new HashSet<>();
            }

            Collection<FilterPredicate> predicates = having.accept(new PredicateExtractionVisitor());

            return predicates.stream()
                    .map(predicate -> query.getSource().getMetricProjection(predicate.getField()))
                    .collect(Collectors.toSet());
        }

        private Set<SQLColumnProjection> extractInnerQueryJoinProjections(FilterExpression expression) {
            Collection<FilterPredicate> predicates = expression.accept(new PredicateExtractionVisitor());

            return predicates.stream()
                    .flatMap(predicate -> {
                        return lookupTable.getResolvedJoinProjections(
                                (SQLTable) metaDataStore.getTable(predicate.getEntityType()),
                                predicate.getField()
                        ).stream();
                    })
                    .collect(Collectors.toSet());
        }

        @Override
        public Queryable visitQueryable(Queryable table) {
            return table;
        }
    }

    public Set<SQLDimensionProjection> getVirtualDims(SQLTable source, FilterExpression expression) {
        if (expression == null) {
            return new HashSet<>();
        }
        Collection<FilterPredicate> predicates = expression.accept(new PredicateExtractionVisitor());

        Set<SQLDimensionProjection> virtualDims = new HashSet<>();
        for (FilterPredicate predicate : predicates) {
            ColumnProjection virtualDim = source.getDimensionProjection(predicate.getField());
            if (virtualDim != null) {
                virtualDims.add(
                        SQLDimensionProjection.builder()
                        .name(virtualDim.getName())
                        .columnType(virtualDim.getColumnType())
                        .valueType(virtualDim.getValueType())
                        .expression(virtualDim.getExpression())
                        .arguments(virtualDim.getArguments())
                        .alias(virtualDim.getAlias())
                        .projected(false)
                        .build());
            }
        }
        return virtualDims;
    }

    //TODO - this must use the nesting logic (rather than just going to the inner query).
    public Set<SQLMetricProjection> getVirtualMetrics(SQLTable source, FilterExpression expression) {
        if (expression == null) {
            return new HashSet<>();
        }
        Collection<FilterPredicate> predicates = expression.accept(new PredicateExtractionVisitor());

        Set<SQLMetricProjection> virtualMeasures = new HashSet<>();
        for (FilterPredicate predicate : predicates) {
            MetricProjection virtualMetric = source.getMetricProjection(predicate.getField());
            if (virtualMetric != null) {
                virtualMeasures.add(
                        SQLMetricProjection.builder()
                                .name(virtualMetric.getName())
                                .columnType(virtualMetric.getColumnType())
                                .valueType(virtualMetric.getValueType())
                                .expression(virtualMetric.getExpression())
                                .arguments(virtualMetric.getArguments())
                                .alias(virtualMetric.getAlias())
                                .projected(false)
                                .build());
            }
        }
        return virtualMeasures;
    }

    @Override
    public boolean canOptimize(Query query, SQLReferenceTable lookupTable) {
        //For simplicity, we will not optimize and already nested query.
        if (query.isNested()) {
            return false;
        }

        //Every column must be nestable.
        if (! query.getColumnProjections().stream()
                .allMatch(ColumnProjection::canNest)) {
            return false;
        }
        //TODO - If any of the group by columns require a join across a toMany relationship,
        //we cannot aggregate with joining first

        //TODO - If a metric requires a join, it must be aggregated post join.  For now, we simply won't optimize.

        //There must be at least one join or there is no reason to optimize.  First check the where clause
        //joins.  There is no need to check having clause or sort because those columns must also be in
        //the projection.
        if (query.getWhereFilter() != null) {
            SubqueryFilterSplitter.SplitFilter splitFilter =
                    SubqueryFilterSplitter.splitFilter(lookupTable, metaDataStore, query.getWhereFilter());

            if (splitFilter.getOuter() != null) {
                return true;
            }
        }

        //Next check the projection for required joins.
        for (ColumnProjection column: query.getColumnProjections()) {
            if (lookupTable.getResolvedJoinProjections(query.getSource(), column.getName()).size() > 0) {
                return true;
            }
        }

        return false;
    }

    @Override
    public Query optimize(Query query, SQLReferenceTable lookupTable) {
        if (! canOptimize(query, lookupTable)) {
            return query;
        }

        return (Query) query.accept(new OptimizerVisitor(lookupTable));
    }

    @SafeVarargs
    private static <ColumnProjection> Set<ColumnProjection> combine(Set<ColumnProjection> ...sets) {
        return Stream.of(sets).flatMap(Set::stream).collect(Collectors.toSet());
    }
}
