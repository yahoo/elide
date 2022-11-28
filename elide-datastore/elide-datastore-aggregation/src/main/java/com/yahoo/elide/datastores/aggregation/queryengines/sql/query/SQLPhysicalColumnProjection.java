/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql.query;

import static com.yahoo.elide.datastores.aggregation.metadata.ColumnContext.applyQuotes;

import com.yahoo.elide.core.request.Argument;
import com.yahoo.elide.core.utils.TypeHelper;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import com.yahoo.elide.datastores.aggregation.metadata.enums.ColumnType;
import com.yahoo.elide.datastores.aggregation.metadata.enums.ValueType;
import com.yahoo.elide.datastores.aggregation.query.ColumnProjection;
import com.yahoo.elide.datastores.aggregation.query.DimensionProjection;
import com.yahoo.elide.datastores.aggregation.query.Queryable;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.SQLDialect;

import lombok.Builder;
import lombok.EqualsAndHashCode;

import java.util.Map;

/**
 * Represents a physical column to be projected as part of a subquery.
 */
@EqualsAndHashCode
@Builder
public class SQLPhysicalColumnProjection implements SQLColumnProjection, DimensionProjection {

    private String name;

    public SQLPhysicalColumnProjection(String name) {
        // Physical Column Reference starts with '$'
        if (name.indexOf('$') == 0) {
            name = name.substring(1);
        }
        this.name = name;
    }

    @Override
    public String toSQL(Queryable query, MetaDataStore metaDataStore) {
        SQLDialect dialect = query.getConnectionDetails().getDialect();
        return TypeHelper.getFieldAlias(applyQuotes(query.getSource().getAlias(), dialect), applyQuotes(name, dialect));
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

    @Override
    public SQLPhysicalColumnProjection withProjected(boolean projected) {
        return new SQLPhysicalColumnProjection(name);
    }

    @Override
    public SQLPhysicalColumnProjection withExpression(String expression, boolean projected) {
        return new SQLPhysicalColumnProjection(name);
    }

    @Override
    public ColumnProjection withArguments(Map<String, Argument> arguments) {
        return new SQLPhysicalColumnProjection(name);
    }
}
