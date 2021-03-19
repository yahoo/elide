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
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLReferenceVisitor;
import lombok.EqualsAndHashCode;

/**
 * Represents a physical column to be projected as part of a subquery.
 */
@EqualsAndHashCode
public class SQLPhysicalColumnProjection implements SQLColumnProjection {

    private String name;
    private SQLDialect dialect;

    public SQLPhysicalColumnProjection(String name, SQLDialect dialect) {
        this.name = name;
        this.dialect = dialect;
    }

    @Override
    public String toSQL(Queryable source, SQLReferenceTable table) {
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
