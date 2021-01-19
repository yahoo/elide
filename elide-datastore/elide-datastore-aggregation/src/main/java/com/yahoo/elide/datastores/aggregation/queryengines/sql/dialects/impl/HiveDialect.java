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

    private static final char COMMA = ',';

    @Override
    public String getDialectType() {
        return "Hive";
    }

    @Override
    public String generateOffsetLimitClause(int offset, int limit) {
        return LIMIT + offset + COMMA + limit;
    }
}
