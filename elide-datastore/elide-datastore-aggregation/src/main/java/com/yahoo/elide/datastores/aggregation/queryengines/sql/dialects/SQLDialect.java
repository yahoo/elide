package com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects;

public interface SQLDialect {

    String getDialectType();

    String generateCountDistinctClause(String dimensions);
}
