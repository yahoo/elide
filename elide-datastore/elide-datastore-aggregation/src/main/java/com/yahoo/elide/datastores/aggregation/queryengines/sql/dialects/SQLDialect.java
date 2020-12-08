/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects;

import com.yahoo.elide.datastores.aggregation.annotation.JoinType;

/**
 * Interface for SQL Dialects used to customize SQL queries for specific persistent storage.
 */
public interface SQLDialect {

    /**
     * Returns the name of the Dialect.
     * @return dialect name.
     */
    String getDialectType();

    /**
     * Checks whether we need to use alias for orderby
     * @return boolean.
     */
    boolean useAliasForOrderByClause();

    /**
     * Generates an SQL clause that requests the count of distinct values for the input dimensions.
     * @param dimensions for which to request a distinct count.
     * @return the SQL clause as a string.
     */
    String generateCountDistinctClause(String dimensions);

    /**
     * Generates required offset and limit clause.
     * @param offset position of the first record.
     * @param limit maximum number of record.
     * @return the offset and limit clause.
     */
    String generateOffsetLimitClause(int offset, int limit);

    /**
     * Provides begin quote required for SQL identifiers.
     * @return begin quote for SQL identifiers.
     */
    char getBeginQuote();

    /**
     * Provides end quote required for SQL identifiers.
     * @return end quote for SQL identifiers.
     */
    char getEndQuote();

    /**
     * Provides keyword for requested Join Type.
     * @param joinType {@link JoinType} enum
     * @return the keyword for provided Join type.
     */
    String getJoinKeyword(JoinType joinType);
}
