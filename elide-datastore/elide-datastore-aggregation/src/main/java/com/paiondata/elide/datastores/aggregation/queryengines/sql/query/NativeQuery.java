/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.datastores.aggregation.queryengines.sql.query;

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
    @Builder.Default
    private String offsetLimitClause = "";

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

    //To avoid lombok javadoc errors.
    public static class NativeQueryBuilder {
    }
}
