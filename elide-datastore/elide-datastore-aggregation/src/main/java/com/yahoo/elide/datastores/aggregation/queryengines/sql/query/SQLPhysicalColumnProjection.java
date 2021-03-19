/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql.query;

import com.yahoo.elide.datastores.aggregation.metadata.enums.ColumnType;
import com.yahoo.elide.datastores.aggregation.metadata.enums.ValueType;
import com.yahoo.elide.datastores.aggregation.query.Queryable;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLReferenceTable;
import lombok.EqualsAndHashCode;

/**
 * Represents a physical column to be projected as part of a subquery.
 */
@EqualsAndHashCode
public class SQLPhysicalColumnProjection implements SQLColumnProjection {

    private String name;

    public SQLPhysicalColumnProjection(String name) {
        this.name = name;
    }

    @Override
    //TODO - we need to quote the alias and name
    public String toSQL(Queryable source, SQLReferenceTable table) {
        return source.getAlias() + "." + name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getExpression() {
        return null;
    }

    @Override
    public ValueType getValueType() {
        return null;
    }

    @Override
    public ColumnType getColumnType() {
        return ColumnType.FIELD;
    }
}
