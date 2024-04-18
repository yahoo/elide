/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.datastores.aggregation.queryengines.sql.dialects.impl;

import com.paiondata.elide.datastores.aggregation.queryengines.sql.dialects.AbstractSqlDialect;
import org.apache.calcite.sql.SqlDialect;
import org.apache.calcite.sql.dialect.PrestoSqlDialect;

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
        return PrestoSqlDialect.DEFAULT;
    }
}
