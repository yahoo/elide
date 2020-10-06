package com.yahoo.elide.datastores.aggregation.queryengines.sql.query.plan;

public interface QueryPlanVisitor<T> {
    public T visitQueryPlan(QueryPlan plan);
    public T visitTableSource(TableSource source);
}
