/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql.query.plan;

import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLTable;

import com.yahoo.elide.datastores.aggregation.queryengines.sql.query.SQLColumnProjection;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.query.SQLMetricProjection;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.query.SQLTimeDimensionProjection;
import com.yahoo.elide.request.Pagination;
import com.yahoo.elide.request.Sorting;

import com.google.common.collect.Sets;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * SQLQueryTemplate contains projections information about a sql query.
 */
@Data
@AllArgsConstructor
public class QueryPlan {

    private Source source;
    private List<SQLMetricProjection> metrics;
    private Set<SQLColumnProjection> nonTimeDimensions;
    private Set<SQLTimeDimensionProjection> timeDimensions;
    private FilterExpression where;
    private FilterExpression having;
    private Sorting sortBy;
    private Pagination pagination;

    public <T> T accept(QueryPlanVisitor<T> visitor) {
        return visitor.visitQueryPlan(this);
    }

    public QueryPlan(Query query) {
        source = new TableSource((SQLTable) query.getTable());
        timeDimensions = query.getTimeDimensions().stream()
                .map(SQLTimeDimensionProjection.class::cast)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        nonTimeDimensions = query.getGroupByDimensions().stream()
                .map(SQLColumnProjection.class::cast)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        metrics = query.getMetrics().stream()
                .map(SQLMetricProjection.class::cast)
                .collect(Collectors.toList());

        where = query.getWhereFilter();
        having = query.getHavingFilter();
        sortBy = query.getSorting();
        pagination = query.getPagination();
    }

    /**
     * Get all GROUP BY dimensions in this query, include time and non-time dimensions.
     *
     * @return all GROUP BY dimensions
     */
    public Set<SQLColumnProjection> getGroupByDimensions() {
        return getTimeDimensions() == null
                ? getNonTimeDimensions()
                : Sets.union(getNonTimeDimensions(), getTimeDimensions());
    }

    /**
     * Merge with other query.
     *
     * @param second other query template
     * @return merged query template
     */
     public QueryPlan merge(QueryPlan second) {
         assert this.getSource().equals(second.getSource());
         assert ((this.getWhere() == null && second.getWhere() == null)
                 || this.getWhere().equals(second.getWhere()));
         assert ((this.getHaving() == null && second.getHaving() == null)
                 || this.getHaving().equals(second.getHaving()));
         assert this.getTimeDimensions().equals(second.getTimeDimensions());
         assert this.getNonTimeDimensions().equals(second.getNonTimeDimensions());
         assert ((this.getSortBy() == null && second.getSortBy() == null)
                 || this.getSortBy().equals(second.getSortBy()));
         assert ((this.getPagination() == null && second.getPagination() == null)
                 || this.getPagination().equals(second.getPagination()));

         QueryPlan first = this;
         List<SQLMetricProjection> merged = new ArrayList<>(first.getMetrics());
         merged.addAll(second.getMetrics());

         return new QueryPlan(
                 first.getSource(),
                 merged,
                 first.getNonTimeDimensions(),
                 first.getTimeDimensions(),
                 first.getWhere(),
                 first.getHaving(),
                 first.getSortBy(),
                 first.getPagination());
     }

    /**
     * Returns the entire list of column projections.
     * @return metrics and dimensions.
     */
     public List<SQLColumnProjection> getColumnProjections() {
         ArrayList<SQLColumnProjection> columnProjections = new ArrayList<>();
         columnProjections.addAll(metrics);
         columnProjections.addAll(nonTimeDimensions);
         if (timeDimensions != null) {
            columnProjections.addAll(timeDimensions);
         }
         return columnProjections;
     }
}
