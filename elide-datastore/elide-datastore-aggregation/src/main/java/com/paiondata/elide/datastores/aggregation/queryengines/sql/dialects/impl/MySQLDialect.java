/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.datastores.aggregation.queryengines.sql.dialects.impl;

import com.paiondata.elide.datastores.aggregation.queryengines.sql.dialects.AbstractSqlDialect;
import org.apache.calcite.sql.SqlDialect;
import org.apache.calcite.sql.dialect.MysqlSqlDialect;

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

    @Override
    public String getFullJoinKeyword() {
        throw new IllegalArgumentException("Full Join is not supported for: " + getDialectType());
    }

    @Override
    public SqlDialect getCalciteDialect() {
        return MysqlSqlDialect.DEFAULT;
    }
}
