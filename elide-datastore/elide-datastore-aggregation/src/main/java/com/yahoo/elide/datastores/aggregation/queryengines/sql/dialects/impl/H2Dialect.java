/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.impl;

import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.AbstractSqlDialect;

/**
 * H2 SQLDialect.
 */
public class H2Dialect extends AbstractSqlDialect {
    @Override
    public String getDialectType() {
        return "H2";
    }

    @Override
    public String generateOffsetLimitClause(int offset, int limit) {
        return LIMIT + limit + SPACE + OFFSET + offset;
    }

    @Override
    public String getFullJoinKeyword() {
        throw new IllegalArgumentException("Full Join is not supported for: " + getDialectType());
    }
}
