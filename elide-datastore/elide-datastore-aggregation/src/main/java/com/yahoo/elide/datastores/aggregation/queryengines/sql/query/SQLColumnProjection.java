/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql.query;

import com.yahoo.elide.datastores.aggregation.metadata.models.Column;
import com.yahoo.elide.datastores.aggregation.query.ColumnProjection;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLReferenceTable;

/**
 * Column projection that can expand the column into a SQL projection fragment.
 * @param <T> Column type of the projection.
 */
public interface SQLColumnProjection<T extends Column> extends ColumnProjection<T> {

    SQLReferenceTable getReferenceTable();

    /**
     * Generate a SQL fragment for this combination column and client arguments.
     * @param queryTemplate The query template.
     * @return
     */
    default String toSQL(SQLQueryTemplate queryTemplate) {
        return getReferenceTable().getResolvedReference(getColumn().getTable(), getColumn().getName());
    }
}
