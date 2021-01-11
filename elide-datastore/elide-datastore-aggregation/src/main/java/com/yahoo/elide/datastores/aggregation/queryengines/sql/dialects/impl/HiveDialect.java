/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.impl;

import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.AbstractSqlDialect;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.query.SQLQuery;

/**
 * Hive SQLDialect.
 */
public class HiveDialect extends AbstractSqlDialect {

    private static final char COMMA = ',';

    @Override
    public String getDialectType() {
        return "Hive";
    }

    /**
     * Omit parentheses on the inner DISTINCT clause.
     * @param dimensions
     * @return
     */
    @Override
    public SQLQuery generateCountDistinctQuery(SQLQuery sql, String dimensions) {
        return SQLQuery.builder()
                .projectionClause(String.format("COUNT(DISTINCT %s)", dimensions))
                .fromClause(sql.getFromClause())
                .joinClause(sql.getJoinClause())
                .whereClause(sql.getWhereClause())
                .havingClause(sql.getHavingClause())
                .build();
    }

    @Override
    public String generateOffsetLimitClause(int offset, int limit) {
        return LIMIT + offset + COMMA + limit;
    }
}
