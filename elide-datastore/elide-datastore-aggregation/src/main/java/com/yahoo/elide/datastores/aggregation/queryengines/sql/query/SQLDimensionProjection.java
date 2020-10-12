/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql.query;

import com.yahoo.elide.datastores.aggregation.metadata.enums.ColumnType;
import com.yahoo.elide.datastores.aggregation.metadata.enums.ValueType;
import com.yahoo.elide.datastores.aggregation.metadata.models.Dimension;
import com.yahoo.elide.datastores.aggregation.query.Queryable;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLReferenceTable;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLTable;
import com.yahoo.elide.request.Argument;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.util.Map;

/**
 * Dimension projection that can expand the dimension into a SQL projection fragment.
 */
@Value
@Builder
@AllArgsConstructor
public class SQLDimensionProjection implements SQLColumnProjection {
    private String id;
    private Queryable source;
    private String name;
    private ValueType valueType;
    private ColumnType columnType;
    private String expression;
    private String alias;
    private Map<String, Argument> arguments;

    public SQLDimensionProjection(Dimension dimension,
                                  String alias,
                                  Map<String, Argument> arguments) {
        this.id = dimension.getId();
        this.source = (SQLTable) dimension.getTable();
        this.name = dimension.getName();
        this.expression = dimension.getExpression();
        this.valueType = dimension.getValueType();
        this.columnType = dimension.getColumnType();
        this.alias = alias;
        this.arguments = arguments;
    }
}
