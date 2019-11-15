/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata;

import static com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLTable.resolveSQLDimensions;

import com.yahoo.elide.datastores.aggregation.AggregationDictionary;
import com.yahoo.elide.datastores.aggregation.metadata.models.AnalyticView;
import com.yahoo.elide.datastores.aggregation.metadata.models.Table;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Set;

/**
 * SQL extension of {@link AnalyticView} which also contains sql column meta data.
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class SQLAnalyticView extends AnalyticView implements SQLTable {
    private Set<SQLColumn> sqlColumns;

    public SQLAnalyticView(Class<?> cls, AggregationDictionary dictionary) {
        super(cls, dictionary);
        this.sqlColumns = resolveSQLDimensions(cls, dictionary);
    }

    @Override
    public Table asTable() {
        return this;
    }
}
