/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql.query;

import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLTable;

import com.google.common.collect.Sets;
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
public class SQLQueryTemplate {

    private final SQLTable table;
    private final List<SQLMetricProjection> metrics;
    private final Set<SQLColumnProjection> nonTimeDimensions;
    private final Set<SQLTimeDimensionProjection> timeDimensions;

    public SQLQueryTemplate(SQLTable table, List<SQLMetricProjection> metrics,
                     Set<SQLColumnProjection> nonTimeDimensions, Set<SQLTimeDimensionProjection> timeDimensions) {
        this.table = table;
        this.nonTimeDimensions = nonTimeDimensions;
        this.timeDimensions = timeDimensions;
        this.metrics = metrics;
    }

    public SQLQueryTemplate(Query query) {
        table = (SQLTable) query.getTable();
        timeDimensions = query.getTimeDimensions().stream()
                .map(SQLTimeDimensionProjection.class::cast)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        nonTimeDimensions = query.getGroupByDimensions().stream()
                .map(SQLColumnProjection.class::cast)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        metrics = query.getMetrics().stream()
                .map(SQLMetricProjection.class::cast)
                .collect(Collectors.toList());
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
     public SQLQueryTemplate merge(SQLQueryTemplate second) {
         // TODO: validate dimension
         assert this.getTable().equals(second.getTable());
         SQLQueryTemplate first = this;
         List<SQLMetricProjection> merged = new ArrayList<>(first.getMetrics());
         merged.addAll(second.getMetrics());

         return new SQLQueryTemplate(first.getTable(), merged, first.getNonTimeDimensions(), first.getTimeDimensions());
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
