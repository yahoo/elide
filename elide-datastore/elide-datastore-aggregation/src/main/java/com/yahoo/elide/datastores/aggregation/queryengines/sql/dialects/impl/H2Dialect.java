package com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.impl;

import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.AbstractSqlDialect;

public class H2Dialect extends AbstractSqlDialect {
    @Override
    public String getDialectType() {
        return "H2";
    }
}
