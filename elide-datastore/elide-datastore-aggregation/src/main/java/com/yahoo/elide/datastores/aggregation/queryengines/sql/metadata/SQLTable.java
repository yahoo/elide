/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.datastores.aggregation.metadata.models.Table;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * SQL extension of {@link Table} which also contains sql column meta data.
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class SQLTable extends Table {
    public SQLTable(Class<?> cls, EntityDictionary dictionary) {
        super(cls, dictionary);
    }

    public final SQLColumn getSQLColumn(String fieldName) {
        SQLDimension dimension = getColumn(SQLDimension.class, fieldName);
        return dimension == null ? getColumn(SQLTimeDimension.class, fieldName) : dimension;
    }

    @Override
    protected SQLMetric constructMetric(Class<?> cls, String fieldName, EntityDictionary dictionary) {
        return new SQLMetric(cls, fieldName, dictionary);
    }

    @Override
    protected SQLTimeDimension constructTimeDimension(Class<?> cls, String fieldName, EntityDictionary dictionary) {
        return new SQLTimeDimension(cls, fieldName, dictionary);
    }

    @Override
    protected SQLDimension constructDimension(Class<?> cls, String fieldName, EntityDictionary dictionary) {
        return new SQLDimension(cls, fieldName, dictionary);
    }
}
