package com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.impl;

import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.AbstractSqlDialect;

public class HiveDialect extends AbstractSqlDialect {
    @Override
    public String getDialectType() {
        return "Hive";
    }

    /**
     * Omit parentheses on the inner DISTINCT clause
     * @param dimensions
     * @return
     */
    @Override
    public String generateCountDistinctClause(String dimensions){
        return String.format("COUNT(DISTINCT %s)", dimensions);
    }

}
