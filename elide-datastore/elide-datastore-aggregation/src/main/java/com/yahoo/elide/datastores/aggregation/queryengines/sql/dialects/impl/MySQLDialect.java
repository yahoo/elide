/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.impl;

import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.AbstractSqlDialect;

/**
 * MySQL SQLDialect.
 */
public class MySQLDialect extends AbstractSqlDialect {

    @Override
    public String getDialectType() {
        return "MySQL";
    }

    @Override
    public String generateOffsetLimitClause(int offset, int limit) {
        return LIMIT + offset + COMMA + limit;
    }
}
