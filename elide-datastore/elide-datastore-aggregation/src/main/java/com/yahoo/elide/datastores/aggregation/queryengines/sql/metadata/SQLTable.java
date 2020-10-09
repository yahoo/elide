/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.datastores.aggregation.metadata.enums.ColumnType;
import com.yahoo.elide.datastores.aggregation.metadata.enums.ValueType;
import com.yahoo.elide.datastores.aggregation.metadata.models.Column;
import com.yahoo.elide.datastores.aggregation.metadata.models.Table;

import com.yahoo.elide.datastores.aggregation.query.ColumnProjection;
import com.yahoo.elide.datastores.aggregation.query.Queryable;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.query.SQLColumnProjection;
import com.yahoo.elide.request.Argument;
import lombok.EqualsAndHashCode;

import java.util.HashMap;
import java.util.Map;

/**
 * SQL extension of {@link Table} which also contains sql column meta data.
 */
@EqualsAndHashCode(callSuper = true)
public class SQLTable extends Table {
    private SQLReferenceTable referenceTable;

    public SQLTable(Class<?> cls, EntityDictionary dictionary, SQLReferenceTable referenceTable) {
        super(cls, dictionary);
        this.referenceTable = referenceTable;
    }

    @Override
    protected SQLMetric constructMetric(String fieldName, EntityDictionary dictionary) {
        return new SQLMetric(this, fieldName, dictionary);
    }

    @Override
    public ColumnProjection getColumnProjection(String name) {
        ColumnProjection projection = super.getColumnProjection(name);

        if (projection == null) {
            return null;
        }

        return new SQLColumnProjection() {
            @Override
            public SQLReferenceTable getReferenceTable() {
                return referenceTable;
            }

            @Override
            public Queryable getSource() {
                return SQLTable.this;
            }

            @Override
            public Column getColumn() {
                return projection.getColumn();
            }

            @Override
            public String getAlias() {
                return projection.getAlias();
            }

            @Override
            public String getId() {
                return projection.getId();
            }

            @Override
            public String getName() {
                return projection.getName();
            }

            @Override
            public String getExpression() {
                return projection.getExpression();
            }

            @Override
            public ValueType getValueType() {
                return projection.getValueType();
            }

            @Override
            public ColumnType getColumnType() {
                return projection.getColumnType();
            }

            @Override
            public Map<String, Argument> getArguments() {
                return new HashMap<>();
            }
        };
    }
}
