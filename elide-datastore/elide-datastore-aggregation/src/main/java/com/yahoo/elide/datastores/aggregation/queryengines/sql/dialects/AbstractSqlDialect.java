/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects;

/**
 * Common code for {@link SQLDialect} implementations
 */
public abstract class AbstractSqlDialect implements SQLDialect {

    public String generateCountDistinctClause(String dimensions){
        return String.format("COUNT(DISTINCT(%s))", dimensions);
    }

}
