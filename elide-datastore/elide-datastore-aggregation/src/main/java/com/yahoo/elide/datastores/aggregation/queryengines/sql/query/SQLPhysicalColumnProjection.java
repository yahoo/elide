/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql.query;

import static com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLReferenceTable.applyQuotes;
import com.yahoo.elide.core.utils.TypeHelper;
import com.yahoo.elide.datastores.aggregation.metadata.enums.ColumnType;
import com.yahoo.elide.datastores.aggregation.metadata.enums.ValueType;
import com.yahoo.elide.datastores.aggregation.query.Queryable;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.SQLDialect;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLReferenceTable;
import lombok.Builder;
import lombok.EqualsAndHashCode;

/**
 * Represents a physical column to be projected as part of a subquery.
 */
@EqualsAndHashCode
@Builder
public class SQLPhysicalColumnProjection implements SQLColumnProjection {

    private final String name;

    public SQLPhysicalColumnProjection(String name) {
        // Physical Column Reference starts with '$'
        if (name.indexOf('$') == 0) {
            name = name.substring(1);
        }
        this.name = name;
    }

    @Override
    public String toSQL(Queryable source, SQLReferenceTable table) {
        SQLDialect dialect = source.getConnectionDetails().getDialect();
        return TypeHelper.getFieldAlias(applyQuotes(source.getAlias(), dialect), applyQuotes(name, dialect));
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
