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
import com.yahoo.elide.datastores.aggregation.query.MetricProjection;
import com.yahoo.elide.datastores.aggregation.query.Optimizer;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.datastores.aggregation.query.QueryVisitor;
import com.yahoo.elide.datastores.aggregation.query.Queryable;
import com.yahoo.elide.datastores.aggregation.query.TimeDimensionProjection;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLReferenceTable;

import com.google.common.collect.Streams;

import org.apache.commons.lang3.tuple.Pair;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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

            Set<ColumnProjection> allProjections = Streams.concat(
                    query.getColumnProjections().stream(),
                    extractFilterProjections(query, splitWhere.getOuter()).stream())
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            Set<Pair<ColumnProjection, Set<ColumnProjection>>> allProjectionsNested = allProjections.stream()
                    .map((projection) -> projection.nest(query, lookupTable, true))
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            Set<ColumnProjection> allOuterProjections = allProjectionsNested.stream()
                    .map(Pair::getLeft)
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            Set<ColumnProjection> allInnerProjections = allProjectionsNested.stream()
                    .map(Pair::getRight)
                    .flatMap(Set::stream)
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            Query inner = Query.builder()
                    .source(query.getSource().accept(this))
                    .metricProjections(allInnerProjections.stream()
                                    .filter((predicate) -> predicate instanceof SQLMetricProjection)
                                    .map(MetricProjection.class::cast)
                                    .collect(Collectors.toCollection(LinkedHashSet::new)))
                    .dimensionProjections(allInnerProjections.stream()
                            .filter((predicate) -> predicate instanceof SQLDimensionProjection
                                    || predicate instanceof SQLPhysicalColumnProjection)
                            .collect(Collectors.toCollection(LinkedHashSet::new)))
                    .timeDimensionProjections(allInnerProjections.stream()
                            .filter((predicate) -> predicate instanceof SQLTimeDimensionProjection)
                            .map(SQLTimeDimensionProjection.class::cast)
                            .collect(Collectors.toCollection(LinkedHashSet::new)))
                    .whereFilter(splitWhere.getInner())
                    .build();

            return Query.builder()
                    .metricProjections(allOuterProjections.stream()
                            .filter((predicate) -> predicate instanceof SQLMetricProjection)
                            .map(MetricProjection.class::cast)
                            .collect(Collectors.toCollection(LinkedHashSet::new)))
                    .dimensionProjections(allOuterProjections.stream()
                                    .filter((predicate) -> predicate instanceof SQLDimensionProjection)
                                    .collect(Collectors.toCollection(LinkedHashSet::new)))
                    .timeDimensionProjections(allOuterProjections.stream()
                            .filter((predicate) -> predicate instanceof SQLTimeDimensionProjection)
                            .map(TimeDimensionProjection.class::cast)
                            .collect(Collectors.toCollection(LinkedHashSet::new)))
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
         * Extracts the columns referenced in a filter expression.
         * @param query The parent query.
         * @param expression The filter expression.
         * @return set of referenced columns.
         */
        private Set<SQLColumnProjection> extractFilterProjections(Query query, FilterExpression expression) {
            if (expression == null) {
                return new LinkedHashSet<>();
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
    public String hint() {
        return "AggregateBeforeJoin";
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
