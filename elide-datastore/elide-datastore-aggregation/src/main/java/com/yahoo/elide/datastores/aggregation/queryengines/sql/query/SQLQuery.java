/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql.query;

import com.yahoo.elide.datastores.aggregation.query.Query;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

/**
 * Aids in constructing a SQL query from String fragments.
 */
@Data
@Builder
public class SQLQuery {

    private static final String SPACE = " ";

    @NonNull
    private Query clientQuery;

    @NonNull
    private String fromClause;

    @NonNull
    private String projectionClause;

    @Builder.Default
    private String joinClause = "";
    @Builder.Default
    private String whereClause = "";
    @Builder.Default
    private String groupByClause = "";
    @Builder.Default
    private String havingClause = "";
    @Builder.Default
    private String orderByClause = "";

    @Override
    public String toString() {
        return String.format("SELECT %s FROM %s", projectionClause, fromClause)
                + SPACE + joinClause
                + SPACE + whereClause
                + SPACE + groupByClause
                + SPACE + havingClause
                + SPACE + orderByClause;
    }
}
