/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql.query;

import com.yahoo.elide.core.request.Argument;
import com.yahoo.elide.datastores.aggregation.metadata.enums.ColumnType;
import com.yahoo.elide.datastores.aggregation.metadata.enums.ValueType;
import com.yahoo.elide.datastores.aggregation.metadata.models.Dimension;
import com.yahoo.elide.datastores.aggregation.query.ColumnProjection;
import com.yahoo.elide.datastores.aggregation.query.DimensionProjection;

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
public class SQLDimensionProjection implements SQLColumnProjection, DimensionProjection {
    private String name;
    private ValueType valueType;
    private ColumnType columnType;
    private String expression;
    private String alias;
    private Map<String, Argument> arguments;
    private boolean projected;

    public SQLDimensionProjection(Dimension dimension,
                                  String alias,
                                  Map<String, Argument> arguments,
                                  boolean projected) {
        this.name = dimension.getName();
        this.expression = dimension.getExpression();
        this.valueType = dimension.getValueType();
        this.columnType = dimension.getColumnType();
        this.alias = alias;
        this.arguments = arguments;
        this.projected = projected;
    }

    @Override
    public SQLDimensionProjection withExpression(String expression, boolean projected) {
        return new SQLDimensionProjection(name, valueType, columnType, expression, alias, arguments, projected);
    }

    @Override
    public boolean isProjected() {
        return projected;
    }

    @Override
    public SQLDimensionProjection withProjected(boolean projected) {
        return new SQLDimensionProjection(name, valueType, columnType, expression, alias, arguments, projected);
    }

    @Override
    public ColumnProjection withArguments(Map<String, Argument> arguments) {
        return new SQLDimensionProjection(name, valueType, columnType, expression, alias, arguments, projected);
    }
}
