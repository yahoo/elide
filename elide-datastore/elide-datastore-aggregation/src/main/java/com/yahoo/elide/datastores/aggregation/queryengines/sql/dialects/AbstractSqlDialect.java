/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects;

/**
 * Common code for {@link SQLDialect} implementations.
 */
public abstract class AbstractSqlDialect implements SQLDialect {

    public static final String OFFSET = " OFFSET ";
    public static final String LIMIT = " LIMIT ";

    @Override
    public boolean useAliasForOrderByClause() {
        return false;
    }

    @Override
    public String generateCountDistinctClause(String dimensions) {
        return String.format("COUNT(DISTINCT(%s))", dimensions);
    }

    @Override
    public String appendOffsetLimit(String sql, int offset, int limit) {
        return sql + OFFSET + offset + LIMIT + limit;
    }
}
