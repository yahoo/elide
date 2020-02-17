/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.datastores.aggregation.metadata.models.Table;

import lombok.EqualsAndHashCode;

/**
 * SQL extension of {@link Table} which also contains sql column meta data.
 */
@EqualsAndHashCode(callSuper = true)
public class SQLTable extends Table {
    public SQLTable(Class<?> cls, EntityDictionary dictionary) {
        super(cls, dictionary);
    }

    public final SQLColumn getSQLColumn(String fieldName) {
        SQLDimension dimension = getColumn(SQLDimension.class, fieldName);
        return dimension == null ? getColumn(SQLTimeDimension.class, fieldName) : dimension;
    }

    @Override
    protected SQLMetric constructMetric(String fieldName, EntityDictionary dictionary) {
        return new SQLMetric(this, fieldName, dictionary);
    }

    @Override
    protected SQLTimeDimension constructTimeDimension(String fieldName, EntityDictionary dictionary) {
        return new SQLTimeDimension(this, fieldName, dictionary);
    }

    @Override
    protected SQLDimension constructDimension(String fieldName, EntityDictionary dictionary) {
        return new SQLDimension(this, fieldName, dictionary);
    }
}
