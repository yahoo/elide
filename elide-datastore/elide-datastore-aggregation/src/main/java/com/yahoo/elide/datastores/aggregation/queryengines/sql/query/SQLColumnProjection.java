/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql.query;

import com.yahoo.elide.core.Path;
import com.yahoo.elide.datastores.aggregation.query.ColumnProjection;
import com.yahoo.elide.datastores.aggregation.query.TimeDimensionProjection;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLColumn;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLTable;

import lombok.Getter;

/**
 * Represents a projected sql column as an alias in a query.
 */
public class SQLColumnProjection implements ColumnProjection {
    @Getter
    private final SQLColumn column;

    @Getter
    private final String alias;

    /**
     * Constructor.
     *
     * @param column The projected sql column
     * @param columnAlias The alias to project this column out.
     */
    public SQLColumnProjection(SQLColumn column, String columnAlias) {
        this.column = column;
        this.alias = columnAlias;
    }

    /**
     * Get the sql join path to this field.
     *
     * @return join path
     */
    public Path getJoinPath() {
        return this.column == null ? null : column.getJoinPath();
    }

    /**
     * Get physical column name of this projection.
     *
     * @return column name
     */
    public String getColumnName() {
        return this.column == null ? getAlias() : column.getColumnName();
    }

    /**
     * Returns a String that identifies this dimension in a SQL query.
     *
     * @return e.g. <code>table_alias.column_name</code>
     */
    public String getColumnReference() {
        return column.getReference();
    }

    /**
     * Build a SQL dimension for a dimension projection.
     *
     * @param projection dimension to project out
     * @param table table that contains this dimension
     * @return constructed dimension
     */
    public static SQLColumnProjection constructSQLProjection(ColumnProjection projection, SQLTable table) {
        String fieldName = projection.getColumn().getName();
        SQLColumn sqlColumn = table.getSQLColumn(fieldName);

        if (projection instanceof TimeDimensionProjection) {
            return new SQLTimeDimensionProjection(
                    sqlColumn,
                    projection.getAlias(),
                    ((TimeDimensionProjection) projection).getTimeDimension(),
                    ((TimeDimensionProjection) projection).getGrain());
        }

        return new SQLColumnProjection(sqlColumn, projection.getAlias());
    }
}
