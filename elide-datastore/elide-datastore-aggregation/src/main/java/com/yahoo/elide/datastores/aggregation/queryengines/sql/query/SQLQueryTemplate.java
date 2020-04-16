/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql.query;

import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLReferenceTable;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLTable;

import com.google.common.collect.Sets;
import lombok.Data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * SQLQueryTemplate contains projections information about a sql query.
 */
@Data
public class SQLQueryTemplate {

    public SQLQueryTemplate(SQLTable table, List<SQLMetricProjection> metrics,
                     Set<SQLColumnProjection> nonTimeDimensions, SQLTimeDimensionProjection timeDimension) {
        this.table = table;
        this.nonTimeDimensions = nonTimeDimensions;
        this.timeDimension = timeDimension;
        this.metrics = metrics;
    }

    public SQLQueryTemplate(Query query, SQLReferenceTable referenceTable) {
        table = (SQLTable) query.getTable();
        timeDimension = query.getTimeDimensions().stream()
                .findFirst()
                .map((timeDim) -> new SQLTimeDimensionProjection(timeDim, referenceTable))
                .orElse(null);

        nonTimeDimensions = query.getGroupByDimensions().stream()
                .map((dim) -> SQLColumnProjection.toSQLColumnProjection(dim, referenceTable))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        metrics = query.getMetrics().stream()
                .map((metric) -> new SQLMetricProjection(metric, referenceTable))
                .collect(Collectors.toList());
    }

    private final SQLTable table;
    private final List<SQLMetricProjection> metrics;
    private final Set<SQLColumnProjection> nonTimeDimensions;
    private final SQLTimeDimensionProjection timeDimension;

    /**
     * Get all GROUP BY dimensions in this query, include time and non-time dimensions.
     *
     * @return all GROUP BY dimensions
     */
    public Set<SQLColumnProjection> getGroupByDimensions() {
        return getTimeDimension() == null
                ? getNonTimeDimensions()
                : Sets.union(getNonTimeDimensions(), Collections.singleton(getTimeDimension()));
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

         return new SQLQueryTemplate(first.getTable(), merged, first.getNonTimeDimensions(), first.getTimeDimension());
     }
}
