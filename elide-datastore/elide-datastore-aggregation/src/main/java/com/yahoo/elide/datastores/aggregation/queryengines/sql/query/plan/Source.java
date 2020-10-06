/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql.query.plan;

import com.yahoo.elide.datastores.aggregation.metadata.models.Queryable;

public interface Source extends Queryable {
    public String getAlias();
    public <T> T accept(QueryPlanVisitor<T> visitor);
}
