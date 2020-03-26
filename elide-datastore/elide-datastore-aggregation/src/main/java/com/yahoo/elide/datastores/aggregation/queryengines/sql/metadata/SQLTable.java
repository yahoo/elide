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

    @Override
    protected SQLMetric constructMetric(String fieldName, EntityDictionary dictionary) {
        return new SQLMetric(this, fieldName, dictionary);
    }
}
