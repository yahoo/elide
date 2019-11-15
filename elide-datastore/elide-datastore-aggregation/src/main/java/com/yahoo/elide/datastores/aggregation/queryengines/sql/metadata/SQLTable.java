/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata;

import com.yahoo.elide.core.exceptions.InternalServerErrorException;
import com.yahoo.elide.datastores.aggregation.AggregationDictionary;
import com.yahoo.elide.datastores.aggregation.metadata.models.Table;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * SQL Table store sql columns with their reference to physical table/views.
 */
public interface SQLTable {
    /**
     * Get sql column meta data.
     *
     * @return all sql columns
     */
    Set<SQLColumn> getSQLColumns();

    /**
     * Get sql column meta data based on field name.
     *
     * @param fieldName field name
     * @return sql column
     */
    default SQLColumn getSQLColumn(String fieldName) {
        return getSQLColumns().stream()
                .filter(col -> col.getName().equals(fieldName))
                .findFirst()
                .orElseThrow(() -> new InternalServerErrorException("SQLField not found: " + fieldName));
    }

    /**
     * Get this table as logical table form.
     *
     * @return logical table object of this table.
     */
    Table asTable();

    /**
     * Resolve all sql columns of a table.
     *
     * @param cls table class
     * @param dictionary dictionary contains the table class
     * @return all resolved sql column metadata
     */
    static Set<SQLColumn> resolveSQLDimensions(Class<?> cls, AggregationDictionary dictionary) {
        return dictionary.getAllFields(cls).stream()
                .filter(field -> !dictionary.isMetricField(cls, field))
                .map(field -> new SQLColumn(cls, field, dictionary))
                .collect(Collectors.toSet());
    }
}
