/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects;

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
     * Generates an SQL clause that requests the count of distinct values for the input dimensions.
     * @param dimensions for which to request a distinct count.
     * @return the SQL clause as a string.
     */
    String generateCountDistinctClause(String dimensions);
}
