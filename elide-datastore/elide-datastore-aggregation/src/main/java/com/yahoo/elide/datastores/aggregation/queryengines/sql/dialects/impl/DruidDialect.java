/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.impl;

import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.AbstractSqlDialect;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.query.SQLQuery;

public class DruidDialect extends AbstractSqlDialect {
    @Override
    public String getDialectType() {
        return "Druid";
    }

    /**
     * Distinct function can only be applied
     * @param dimensions
     * @return
     */
    @Override
    public SQLQuery generateCountDistinctQuery(SQLQuery sql,  String dimensions) {
        SQLQuery innerQuery =  SQLQuery.builder()
                .projectionClause(dimensions)
                .fromClause(sql.getFromClause())
                .joinClause(sql.getJoinClause())
                .whereClause(sql.getWhereClause())
                .groupByClause(String.format("GROUP BY %s", dimensions))
                .havingClause(sql.getHavingClause())
                .build();

        return SQLQuery.builder()
                .projectionClause("COUNT(*)")
                .fromClause(String.format("(%s) AS %sdist_dims%s",
                        innerQuery.toString(), getBeginQuote(), getEndQuote()))
                .build();
    }

    @Override
    public boolean useAliasForOrderByClause() {
        return true;
    }

    @Override
    public String generateOffsetLimitClause(int offset, int limit) {
        return LIMIT + limit + SPACE + OFFSET + offset;
    }

    @Override
    public char getBeginQuote() {
        return DOUBLE_QUOTE;
    }

    @Override
    public char getEndQuote() {
        return DOUBLE_QUOTE;
    }
}
