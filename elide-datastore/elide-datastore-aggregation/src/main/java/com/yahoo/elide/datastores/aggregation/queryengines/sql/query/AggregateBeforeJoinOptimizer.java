/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql.query;

import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.expression.PredicateExtractionVisitor;
import com.yahoo.elide.core.filter.predicates.FilterPredicate;
import com.yahoo.elide.core.request.Argument;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import com.yahoo.elide.datastores.aggregation.query.ColumnProjection;
import com.yahoo.elide.datastores.aggregation.query.Optimizer;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.datastores.aggregation.query.QueryVisitor;
import com.yahoo.elide.datastores.aggregation.query.Queryable;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLReferenceTable;

import com.google.common.collect.Sets;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

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
                    .metricProjections(getInnerQueryColumns(query, SQLMetricProjection.class))
                    .dimensionProjections(Sets.union(

                            //Fetch just the projected dimensions.
                            getInnerQueryColumns(query, SQLDimensionProjection.class),

                            //Fetch all columns that nest as physical column projections.
                            getInnerQueryColumns(query, SQLColumnProjection.class,
                                    (predicate) -> predicate instanceof SQLPhysicalColumnProjection))
                    )
                    .timeDimensionProjections(getInnerQueryColumns(query, SQLTimeDimensionProjection.class))
                    .whereFilter(splitWhere.getInner())
                    .build();

            return Query.builder()
                    .metricProjections(getOuterQueryColumns(query, SQLMetricProjection.class))
                    .dimensionProjections(getOuterQueryColumns(query, SQLDimensionProjection.class))
                    .timeDimensionProjections(getOuterQueryColumns(query, SQLTimeDimensionProjection.class))
                    .whereFilter(splitWhere.getOuter())
                    .havingFilter(query.getHavingFilter())
                    .sorting(query.getSorting())
                    .pagination(query.getPagination())
                    .scope(query.getScope())
                    .bypassingCache(query.isBypassingCache())
                    .source(inner)
                    .build();
        }

        private <T extends ColumnProjection> Set<T> getInnerQueryColumns(Query query, Class<T> columnType,
                                                                         Predicate<? super ColumnProjection> filter) {
            Set<T> projections = new LinkedHashSet<>();

            query.getColumnProjections().stream()
                    .filter(projection -> columnType.isInstance(projection))
                    .flatMap(projection -> projection.innerQuery(query, lookupTable).stream())
                    .filter(filter)
                    .map(columnType::cast)
                    .forEach(projections::add);

            extractFilterProjections(query, query.getWhereFilter()).stream()
                    .filter(projection -> columnType.isInstance(projection))
                    .flatMap(projection -> projection.innerQuery(query, lookupTable).stream())
                    .filter(filter)
                    .map(columnType::cast)
                    .forEach(projections::add);

            extractFilterProjections(query, query.getHavingFilter()).stream()
                    .filter(projection -> columnType.isInstance(projection))
                    .flatMap(projection -> projection.innerQuery(query, lookupTable).stream())
                    .filter(filter)
                    .map(columnType::cast)
                    .forEach(projections::add);

            //TODO - Sorting

            return projections;

        }

        private <T extends ColumnProjection> Set<T> getInnerQueryColumns(Query query, Class<T> columnType) {
            return getInnerQueryColumns(query, columnType, (projection) -> columnType.isInstance(projection));
        }

        private <T extends ColumnProjection> Set<T> getOuterQueryColumns(Query query, Class<T> columnType) {
            Set<T> projections = new LinkedHashSet<>();

            query.getColumnProjections().stream()
                    .filter(projection -> columnType.isInstance(projection))
                    .map(projection -> projection.outerQuery(query, lookupTable))
                    .map(columnType::cast)
                    .forEach(projections::add);

            extractFilterProjections(query, query.getWhereFilter()).stream()
                    .filter(projection -> columnType.isInstance(projection))
                    .map(projection -> projection.outerQuery(query, lookupTable))
                    .map(columnType::cast)
                    .forEach(projections::add);

            extractFilterProjections(query, query.getHavingFilter()).stream()
                    .filter(projection -> columnType.isInstance(projection))
                    .map(projection -> projection.outerQuery(query, lookupTable))
                    .map(columnType::cast)
                    .forEach(projections::add);

            //TODO - Sorting
            return projections;
        }

        private Set<SQLColumnProjection> extractFilterProjections(Query query, FilterExpression expression) {
            if (expression == null) {
                return new HashSet<>();
            }

            Collection<FilterPredicate> predicates = expression.accept(new PredicateExtractionVisitor());

            Set<SQLColumnProjection> filterProjections = new LinkedHashSet<>();
            predicates.stream().forEach((predicate -> {
                Map<String, Argument> arguments = new HashMap<>();
                predicate.getParameters().forEach((param) -> {
                    arguments.put(param.getName(), Argument.builder()
                            .name(param.getName())
                            .value(param.getValue())
                            .build());

                });

                ColumnProjection projection = query.getSource().getColumnProjection(predicate.getField(), arguments);

                filterProjections.add((SQLColumnProjection) projection);
            }));

            return filterProjections;
        }

       @Override
        public Queryable visitQueryable(Queryable table) {
            return table;
        }
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
}
