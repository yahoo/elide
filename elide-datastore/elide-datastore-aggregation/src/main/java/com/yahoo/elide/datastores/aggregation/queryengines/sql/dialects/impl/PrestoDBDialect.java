/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.impl;

import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.AbstractSqlDialect;
import org.apache.calcite.avatica.util.Casing;
import org.apache.calcite.config.Lex;
import org.apache.calcite.sql.SqlDialect;

/**
 * PrestoDB SQLDialect.
 */
public class PrestoDBDialect extends AbstractSqlDialect {
    @Override
    public String getDialectType() {
        return "PrestoDB";
    }

    @Override
    public boolean useAliasForOrderByClause() {
        return true;
    }

    @Override
    public String generateOffsetLimitClause(int offset, int limit) {
        // offset is supported in prestosql but not in prestodb
        return LIMIT + limit;
    }

    @Override
    public char getBeginQuote() {
        return DOUBLE_QUOTE;
    }

    @Override
    public char getEndQuote() {
        return DOUBLE_QUOTE;
    }

    @Override
    public SqlDialect getCalciteDialect() {
        return new SqlDialect(SqlDialect.EMPTY_CONTEXT
                .withIdentifierQuoteString(String.valueOf(DOUBLE_QUOTE))
                .withLiteralQuoteString(String.valueOf(DOUBLE_QUOTE))
                .withLiteralEscapedQuoteString(String.valueOf(DOUBLE_QUOTE))
                .withCaseSensitive(true)
                .withQuotedCasing(Casing.UNCHANGED)
                .withUnquotedCasing(Casing.UNCHANGED));
    }

    @Override
    public Lex getCalciteLex() {
        return Lex.MYSQL_ANSI;
    }
}
