/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects;

import com.yahoo.elide.datastores.aggregation.annotation.JoinType;

/**
 * Common code for {@link SQLDialect} implementations.
 */
public abstract class AbstractSqlDialect implements SQLDialect {

    public static final String OFFSET = "OFFSET ";
    public static final String LIMIT = "LIMIT ";
    public static final char BACKTICK = '`';
    public static final char DOUBLE_QUOTE = '"';
    public static final char SPACE = ' ';
    public static final char COMMA = ',';
    public static final String LEFT_JOIN_SYNTAX = "LEFT OUTER JOIN";
    public static final String INNER_JOIN_SYNTAX = "INNER JOIN";
    public static final String FULL_JOIN_SYNTAX = "FULL OUTER JOIN";
    public static final String CROSS_JOIN_SYNTAX = "CROSS JOIN";

    @Override
    public boolean useAliasForOrderByClause() {
        return false;
    }

    @Override
    public String generateCountDistinctClause(String dimensions) {
        return String.format("COUNT(DISTINCT(%s))", dimensions);
    }

    @Override
    public String generateOffsetLimitClause(int offset, int limit) {
        return OFFSET + offset + SPACE + LIMIT + limit;
    }

    @Override
    public char getBeginQuote() {
        return BACKTICK;
    }

    @Override
    public char getEndQuote() {
        return BACKTICK;
    }

    @Override
    public String getJoinKeyword(JoinType joinType) {

        switch (joinType) {
            case LEFT:
                return getLeftJoinKeyword();
            case INNER:
                return getInnerJoinKeyword();
            case FULL:
                return getFullJoinKeyword();
            case CROSS:
                return getCrossJoinKeyword();
            default:
                return getLeftJoinKeyword();
        }
    }

    public String getLeftJoinKeyword() {
        return LEFT_JOIN_SYNTAX;
    }

    public String getInnerJoinKeyword() {
        return INNER_JOIN_SYNTAX;
    }

    public String getFullJoinKeyword() {
        return FULL_JOIN_SYNTAX;
    }

    public String getCrossJoinKeyword() {
        return CROSS_JOIN_SYNTAX;
    }
}
