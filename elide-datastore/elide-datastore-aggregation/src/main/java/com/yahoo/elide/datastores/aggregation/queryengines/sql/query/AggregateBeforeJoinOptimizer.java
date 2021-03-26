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

/**
 * This optimizer attempts to aggregate data prior to table joins by nesting the query into an inner query
 * (where aggregation occurs) and an outer query (where joins occur).  This optimization works well when the
 * join is not sparse (common case) but may underperform if the join is sparse (in which case joining first and
 * then aggregating is preferred).
 *
 * It should be noted that this kind of query nesting is currently different than (and not compatible with) nesting
 * that is done during query planning.  Query planning currently takes the opposite approach - perform joins in the
 * inner query and avoid them in the outer query.
 */
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

        /**
         * Extracts all inner query columns of a particular column type (metric, dimension, time dimension)
         * that match the given filter.
         * @param query The query to extract columns from.
         * @param columnType The column type of columns to extract.
         * @param filter The filter to apply
         * @param <T> The column type of columns to extract.
         * @return A set of extracted columns.
         */
        private <T extends ColumnProjection> Set<T> getInnerQueryColumns(Query query, Class<T> columnType,
                                                                         Predicate<? super ColumnProjection> filter) {
            Set<T> projections = new LinkedHashSet<>();

            //This covers having & sort clauses where the columns are also part of the projection.
            query.getColumnProjections().stream()
                    .filter(projection -> columnType.isInstance(projection))
                    .flatMap(projection -> projection.innerQuery(query, lookupTable, true).stream())
                    .filter(filter)
                    .map(columnType::cast)
                    .forEach(projections::add);

            //This covers having & sort clauses where the columns are also part of the projection.
            extractFilterProjections(query, query.getWhereFilter()).stream()
                    .filter(projection -> columnType.isInstance(projection))
                    .flatMap(projection -> projection.innerQuery(query, lookupTable, true).stream())
                    .filter(filter)
                    .map(columnType::cast)
                    .forEach(projections::add);

            //TODO - Remove this.  Technically, Having and Sort clauses are covered above but the
            //tests include HAVING clause on a column which is not in the projection (even though the validator
            //would reject the query).
            extractFilterProjections(query, query.getHavingFilter()).stream()
                    .filter(projection -> columnType.isInstance(projection))
                    .flatMap(projection -> projection.innerQuery(query, lookupTable, true).stream())
                    .filter(filter)
                    .map(columnType::cast)
                    .forEach(projections::add);

            return projections;
        }

        private <T extends ColumnProjection> Set<T> getInnerQueryColumns(Query query, Class<T> columnType) {
            return getInnerQueryColumns(query, columnType, (projection) -> columnType.isInstance(projection));
        }

        private <T extends ColumnProjection> Set<T> getOuterQueryColumns(Query query, Class<T> columnType) {
            Set<T> projections = new LinkedHashSet<>();

            //This covers having & sort clauses where the columns are also part of the projection.
            query.getColumnProjections().stream()
                    .filter(projection -> columnType.isInstance(projection))
                    .map(projection -> projection.outerQuery(query, lookupTable, true))
                    .map(columnType::cast)
                    .forEach(projections::add);

            extractFilterProjections(query, query.getWhereFilter()).stream()
                    .filter(projection -> columnType.isInstance(projection))
                    .map(projection -> projection.outerQuery(query, lookupTable, true))
                    .map(columnType::cast)
                    .forEach(projections::add);

            //TODO - Remove this.  Technically, Having and Sort clauses are covered above but the
            //tests include HAVING clause on a column which is not in the projection (even though the validator
            //would reject the query).
            extractFilterProjections(query, query.getHavingFilter()).stream()
                    .filter(projection -> columnType.isInstance(projection))
                    .map(projection -> projection.outerQuery(query, lookupTable, true))
                    .map(columnType::cast)
                    .forEach(projections::add);

            return projections;
        }

        /**
         * Extracts the columns referenced in a filter expression.
         * @param query The parent query.
         * @param expression The filter expression.
         * @return set of referenced columns.
         */
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
        //For simplicity, we will not optimize an already nested query.
        if (query.isNested()) {
            return false;
        }

        //Every column must be nestable.
        if (! query.getColumnProjections().stream()
                .allMatch((projection) -> projection.canNest(query, lookupTable))) {
            return false;
        }

        //TODO - If any of the group by columns require a join across a toMany relationship,
        //we cannot aggregate with joining first

        //TODO - If a metric requires a join, the join could be required prior to aggregation or after aggregation
        //depending on how the join is referenced in the SQL expression.  This requires a more complex understanding
        //of the native SQL expression and outside the scope.  This will require Calcite parsing and also template
        //substitution.

        //There must be at least one join or there is no reason to optimize.  First check the where clause
        //joins.  There is no need to check having clause or sort because those columns must also be in
        //the projection and we check projections below.
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
