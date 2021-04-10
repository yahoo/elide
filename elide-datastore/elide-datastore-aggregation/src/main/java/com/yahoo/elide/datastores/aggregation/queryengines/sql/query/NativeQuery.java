/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql.query;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

/**
 * Aids in constructing a SQL query from String fragments.
 */
@Data
@Builder
public class NativeQuery {

    private static final String SPACE = " ";

    @NonNull
    private final String fromClause;

    @NonNull
    private final String projectionClause;

    @Builder.Default
    private final String joinClause = "";
    @Builder.Default
    private final String whereClause = "";
    @Builder.Default
    private final String groupByClause = "";
    @Builder.Default
    private final String havingClause = "";
    @Builder.Default
    private final String orderByClause = "";
    @Builder.Default
    private final String offsetLimitClause = "";

    @Override
    public String toString() {
        return String.format("SELECT %s FROM %s", projectionClause, fromClause)
                + SPACE + joinClause
                + SPACE + whereClause
                + SPACE + groupByClause
                + SPACE + havingClause
                + SPACE + orderByClause
                + SPACE + offsetLimitClause;
    }
}
