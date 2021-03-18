/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql.query;

import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.expression.PredicateExtractionVisitor;
import com.yahoo.elide.core.filter.predicates.FilterPredicate;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import com.yahoo.elide.datastores.aggregation.query.ColumnProjection;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.datastores.aggregation.query.QueryVisitor;
import com.yahoo.elide.datastores.aggregation.query.Queryable;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLReferenceTable;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLTable;

import com.google.common.collect.Sets;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class QueryOptimizer implements QueryVisitor<Queryable> {

    private MetaDataStore metaDataStore;
    private SQLReferenceTable lookupTable;

    public QueryOptimizer(MetaDataStore metaDataStore, SQLReferenceTable lookupTable) {
        this.lookupTable = lookupTable;
        this.metaDataStore = metaDataStore;
    }

    @Override
    public Queryable visitQuery(Query query) {
        FilterExpression whereFilter = query.getWhereFilter();

        //If there is no filter, there is nothing to optimize
        if (whereFilter == null) {
            Queryable newSource = query.getSource().accept(this);
            return copy(query, newSource);
        }

        if (! query.getColumnProjections().stream()
                .map(SQLColumnProjection.class::cast)
                .allMatch(SQLColumnProjection::canNest)) {
            Queryable newSource = query.getSource().accept(this);
            return copy(query, newSource);
        }

        for (ColumnProjection projection : query.getColumnProjections()) {
            Set<SQLColumnProjection> joins = lookupTable.getResolvedJoinProjections(query, projection.getName());

            //If any of the requested columns require a joins, skip the optimization.
            if (joins.size() > 0) {
                Queryable newSource = query.getSource().accept(this);
                return copy(query, newSource);
            }
        }

        SubqueryFilterSplitter.SplitFilter splitFilter =
                SubqueryFilterSplitter.splitFilter(lookupTable, metaDataStore, whereFilter);

        if (splitFilter.outer == null) {
            //Nothing to optimize as there is no join.
            Queryable newSource = query.getSource().accept(this);
            return copy(query, newSource);
        }

        Query inner = Query.builder()
                .source(query.getSource().accept(this))
                .metricProjections(innerQueryProjections(query.getMetricProjections()))
                .dimensionProjections(Sets.union(
                        innerQueryProjections(query.getDimensionProjections()),
                        extractInnerQueryJoinProjections(splitFilter.getOuter()))
                )
                .timeDimensionProjections(innerQueryProjections(query.getTimeDimensionProjections()))
                .whereFilter(splitFilter.getInner())
                .build();

        return Query.builder()
                .metricProjections(outerQueryProjections(query.getMetricProjections()))
                .dimensionProjections(outerQueryProjections(query.getDimensionProjections()))
                .timeDimensionProjections(outerQueryProjections(query.getTimeDimensionProjections()))
                .whereFilter(splitFilter.getOuter())
                //TODO - Evaluate having filter
                .havingFilter(query.getHavingFilter())
                .sorting(query.getSorting())
                .pagination(query.getPagination())
                .scope(query.getScope())
                .bypassingCache(query.isBypassingCache())
                .source(inner)
                .build();
    }

    private Set<SQLColumnProjection> extractInnerQueryJoinProjections(FilterExpression expression) {
        Collection<FilterPredicate> predicates = expression.accept(new PredicateExtractionVisitor());

        return predicates.stream()
                .flatMap(predicate -> {
                    return lookupTable.getResolvedJoinProjections(
                            (SQLTable) metaDataStore.getTable(predicate.getFieldType()),
                            predicate.getField()
                    ).stream();
                })
                .collect(Collectors.toSet());
    }

    @Override
    public Queryable visitQueryable(Queryable table) {
        return table;
    }

    private Query copy(Query query, Queryable newSource) {
        return Query.builder()
                .metricProjections(query.getMetricProjections())
                .dimensionProjections(query.getDimensionProjections())
                .timeDimensionProjections(query.getTimeDimensionProjections())
                .whereFilter(query.getWhereFilter())
                .havingFilter(query.getHavingFilter())
                .pagination(query.getPagination())
                .sorting(query.getSorting())
                .scope(query.getScope())
                .bypassingCache(query.isBypassingCache())
                .source(newSource)
                .build();
    }

    private static <T extends ColumnProjection> Set<T> outerQueryProjections(Set<T> columns) {
        return (Set<T>) columns.stream()
                .map(SQLColumnProjection.class::cast)
                .map(SQLColumnProjection::outerQuery)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static <T extends ColumnProjection> Set<T> innerQueryProjections(Set<T> columns) {
        return (Set<T>) columns.stream()
                .map(SQLColumnProjection.class::cast)
                .flatMap(projection -> projection.innerQuery().stream())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
