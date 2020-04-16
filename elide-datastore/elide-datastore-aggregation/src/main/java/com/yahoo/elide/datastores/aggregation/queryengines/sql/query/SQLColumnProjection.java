/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql.query;

import com.yahoo.elide.datastores.aggregation.metadata.models.Column;
import com.yahoo.elide.datastores.aggregation.query.ColumnProjection;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLReferenceTable;
import com.yahoo.elide.request.Argument;

import java.util.Map;

/**
 * Column projection that can expand the column into a SQL projection fragment.
 * @param <T> Column type of the projection.
 */
public interface SQLColumnProjection<T extends Column> extends ColumnProjection<T> {

    SQLReferenceTable getReferenceTable();

    default String toSQL() {
        return getReferenceTable().getResolvedReference(getColumn().getTable(), getColumn().getName());
    }

    public static SQLColumnProjection toSQLColumnProjection(ColumnProjection projection,
                                                            SQLReferenceTable referenceTable) {
        return new SQLColumnProjection() {
            @Override
            public SQLReferenceTable getReferenceTable() {
                return referenceTable;
            }

            @Override
            public Column getColumn() {
                return projection.getColumn();
            }

            @Override
            public String getAlias() {
                return projection.getAlias();
            }

            @Override
            public Map<String, Argument> getArguments() {
                return projection.getArguments();
            }
        };
    }
}
