/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql.query.plan;

public interface QueryPlanVisitor<T> {
    public T visitQueryPlan(QueryPlan plan);
    public T visitTableSource(TableSource source);
}
