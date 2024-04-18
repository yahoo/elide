/*
 * Copyright 2022, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.datastores.aggregation.query;

@FunctionalInterface
public interface TableSQLMaker {
    /**
     * Constructs dynamic SQL given a specific client query.
     * @param clientQuery the client query.
     * @return A templated SQL query
     */
    String make(Query clientQuery);
}
