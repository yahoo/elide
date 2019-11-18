/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata;

import com.yahoo.elide.datastores.aggregation.AggregationDictionary;
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

    public SQLTable(Class<?> cls, AggregationDictionary dictionary) {
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
    public static Map<String, SQLColumn> resolveSQLDimensions(Class<?> cls, AggregationDictionary dictionary) {
        return dictionary.getAllFields(cls).stream()
                .filter(field -> !dictionary.isMetricField(cls, field))
                .collect(Collectors.toMap(Function.identity(), field -> new SQLColumn(cls, field, dictionary)));
    }

    public SQLColumn getColumn(String fieldName) {
        return sqlColumns.get(fieldName);
    }
}
