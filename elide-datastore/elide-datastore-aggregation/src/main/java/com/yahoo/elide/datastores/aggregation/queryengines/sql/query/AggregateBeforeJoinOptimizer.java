/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql.query;

import static com.yahoo.elide.datastores.aggregation.query.ColumnProjection.innerQueryProjections;
import static com.yahoo.elide.datastores.aggregation.query.ColumnProjection.outerQueryProjections;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.expression.PredicateExtractionVisitor;
import com.yahoo.elide.core.filter.predicates.FilterPredicate;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import com.yahoo.elide.datastores.aggregation.query.ColumnProjection;
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
            FilterExpression whereFilter = query.getWhereFilter();
            SubqueryFilterSplitter.SplitFilter splitFilter =
                    SubqueryFilterSplitter.splitFilter(lookupTable, metaDataStore, whereFilter);

            Query inner = Query.builder()
                    .source(query.getSource().accept(this))
                    .metricProjections(innerQueryProjections(query.getMetricProjections()))
                    //TODO join projections may either be time dimensions OR regular dimensions - we
                    //should split accordingly
                    .dimensionProjections(Sets.union(
                            Sets.union(innerQueryProjections(query.getDimensionProjections()),
                                    extractInnerQueryJoinProjections(query)),
                            extractInnerQueryJoinProjections(splitFilter.getOuter()))
                    )
                    .timeDimensionProjections(innerQueryProjections(query.getTimeDimensionProjections()))
                    .whereFilter(splitFilter.getInner())
                    .build();

            return Query.builder()
                    .metricProjections(outerQueryProjections(query.getMetricProjections()))
                    .dimensionProjections(Sets.union(
                            outerQueryProjections(query.getDimensionProjections()),
                            getVirtualDims((SQLTable) query.getSource(), splitFilter.getOuter())
                            ))
                    .timeDimensionProjections(outerQueryProjections(query.getTimeDimensionProjections()))
                    .whereFilter(splitFilter.getOuter())
                    .havingFilter(query.getHavingFilter())
                    .sorting(query.getSorting())
                    .pagination(query.getPagination())
                    .scope(query.getScope())
                    .bypassingCache(query.isBypassingCache())
                    .source(inner)
                    .build();
        }

        private Set<SQLColumnProjection> extractInnerQueryJoinProjections(Query query) {
            return query.getColumnProjections().stream()
                    .flatMap(column -> {
                        return lookupTable.getResolvedJoinProjections(query.getSource(), column.getName()).stream();
                    })
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
                        .virtual(true)
                        .build());
            }
        }
        return virtualDims;
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
}
