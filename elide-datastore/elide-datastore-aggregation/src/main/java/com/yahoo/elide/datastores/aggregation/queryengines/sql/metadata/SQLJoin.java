/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata;

import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.datastores.aggregation.annotation.JoinType;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
/**
 * Forms relationships between two SQLTables.
 */
public class SQLJoin {
    private String name;
    private JoinType joinType;
    private boolean toOne;
    private Type<?> joinTableType;
    private String joinExpression;
}
