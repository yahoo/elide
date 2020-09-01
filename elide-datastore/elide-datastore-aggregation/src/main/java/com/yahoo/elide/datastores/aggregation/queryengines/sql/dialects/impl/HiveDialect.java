/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.impl;

import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.AbstractSqlDialect;

/**
 * Hive SQLDialect.
 */
public class HiveDialect extends AbstractSqlDialect {
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
    public String generateCountDistinctClause(String dimensions) {
        return String.format("COUNT(DISTINCT %s)", dimensions);
    }

    @Override
    public String appendOffsetLimit(String sql, int offset, int limit) {
        return sql + " LIMIT " + offset + "," + limit;
    }
}
