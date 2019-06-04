/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata;

import static com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore.isMetricField;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.datastores.aggregation.metadata.models.Column;
import com.yahoo.elide.datastores.aggregation.metadata.models.Table;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * SQL extension of {@link Table} which also contains sql column meta data.
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class SQLTable extends Table {
    private Map<String, SQLColumn> sqlColumns;

    public SQLTable(Class<?> cls, EntityDictionary dictionary) {
        super(cls, dictionary);
        this.sqlColumns = resolveSQLDimensions(cls, dictionary);
    }

    /**
     * Resolve all sql columns of a table.
     *
     * @param cls table class
     * @param dictionary dictionary contains the table class
     * @return all resolved sql column metadata
     */
    public static Map<String, SQLColumn> resolveSQLDimensions(Class<?> cls, EntityDictionary dictionary) {
        return dictionary.getAllFields(cls).stream()
                .filter(field -> Column.getDataType(cls, field, dictionary) != null)
                .filter(field -> !isMetricField(dictionary, cls, field))
                .collect(Collectors.toMap(Function.identity(), field -> new SQLColumn(cls, field, dictionary)));
    }

    public SQLColumn getColumn(String fieldName) {
        return sqlColumns.get(fieldName);
    }
}
