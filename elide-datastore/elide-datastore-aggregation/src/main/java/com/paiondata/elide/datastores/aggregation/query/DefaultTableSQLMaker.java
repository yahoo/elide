/*
 * Copyright 2022, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.datastores.aggregation.query;

import com.paiondata.elide.datastores.aggregation.queryengines.sql.annotation.FromSubquery;
import com.paiondata.elide.datastores.aggregation.queryengines.sql.metadata.SQLTable;

/**
 * Default table SQL maker that just returns the SQL defined in the FromSubquery annotation.
 */
public class DefaultTableSQLMaker implements TableSQLMaker {

    @Override
    public String make(Query clientQuery) {
        SQLTable table = (SQLTable) clientQuery.getRoot();

        FromSubquery fromSubquery = table.getCls().getAnnotation(FromSubquery.class);

        return fromSubquery.sql();
    }
}
