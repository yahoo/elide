/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.datastores.aggregation.queryengines.sql.dialects.impl;

import com.paiondata.elide.datastores.aggregation.queryengines.sql.dialects.AbstractSqlDialect;
import org.apache.calcite.config.NullCollation;
import org.apache.calcite.sql.SqlDialect;

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

    @Override
    public SqlDialect getCalciteDialect() {
        return new SqlDialect(SqlDialect.EMPTY_CONTEXT
                .withIdentifierQuoteString(String.valueOf(getBeginQuote()))
                .withDatabaseProduct(SqlDialect.DatabaseProduct.HIVE)
                .withNullCollation(NullCollation.LOW));
    }
}
