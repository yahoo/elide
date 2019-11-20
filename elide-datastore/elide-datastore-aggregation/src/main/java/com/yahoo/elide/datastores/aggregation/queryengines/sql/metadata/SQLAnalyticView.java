/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata;

import static com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLTable.resolveSQLDimensions;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.datastores.aggregation.metadata.models.AnalyticView;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

/**
 * SQL extension of {@link AnalyticView} which also contains sql column meta data.
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class SQLAnalyticView extends AnalyticView {
    private Map<String, SQLColumn> sqlColumns;

    public SQLAnalyticView(Class<?> cls, EntityDictionary dictionary) {
        super(cls, dictionary);
        this.sqlColumns = resolveSQLDimensions(cls, dictionary);
    }

    public SQLColumn getColumn(String fieldName) {
        return sqlColumns.get(fieldName);
    }
}
