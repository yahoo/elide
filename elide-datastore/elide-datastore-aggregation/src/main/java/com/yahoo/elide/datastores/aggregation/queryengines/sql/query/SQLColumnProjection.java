/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql.query;

import com.yahoo.elide.datastores.aggregation.query.ColumnProjection;
import com.yahoo.elide.datastores.aggregation.query.Queryable;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLReferenceTable;

/**
 * Column projection that can expand the column into a SQL projection fragment.
 */
public interface SQLColumnProjection extends ColumnProjection {

    SQLReferenceTable getReferenceTable();

    /**
     * Generate a SQL fragment for this combination column and client arguments.
     * @param query The SQL query this column is referenced from.
     * @return
     */
    default String toSQL(Queryable query) {
        return getReferenceTable().getResolvedReference(getSource(), getName());
    }
}
