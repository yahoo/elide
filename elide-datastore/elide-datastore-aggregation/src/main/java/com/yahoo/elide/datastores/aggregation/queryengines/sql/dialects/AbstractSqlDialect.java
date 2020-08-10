package com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects;

public abstract class AbstractSqlDialect implements SQLDialect {

    public String generateCountDistinctClause(String dimensions){
        return String.format("COUNT(DISTINCT(%s))", dimensions);
    }

}
