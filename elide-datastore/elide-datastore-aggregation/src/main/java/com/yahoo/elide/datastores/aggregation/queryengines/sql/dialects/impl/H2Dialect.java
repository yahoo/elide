/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.impl;

import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.AbstractSqlDialect;
import org.apache.calcite.avatica.util.Casing;
import org.apache.calcite.sql.SqlDialect;

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

    @Override
    public SqlDialect getCalciteDialect() {
        return new SqlDialect(SqlDialect.EMPTY_CONTEXT
                .withIdentifierQuoteString(String.valueOf(getBeginQuote()))
                .withQuotedCasing(Casing.UNCHANGED)
                .withUnquotedCasing(Casing.UNCHANGED)
                .withDatabaseProduct(SqlDialect.DatabaseProduct.H2));
    }
}
