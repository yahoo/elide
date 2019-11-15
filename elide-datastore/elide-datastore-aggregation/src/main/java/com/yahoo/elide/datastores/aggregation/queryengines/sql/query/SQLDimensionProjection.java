/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql.query;

import com.yahoo.elide.core.Path;
import com.yahoo.elide.datastores.aggregation.query.DimensionProjection;
import com.yahoo.elide.datastores.aggregation.query.TimeDimensionProjection;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLColumn;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata.SQLTable;

import lombok.Getter;

/**
 * Physical Dimension in a sql table.
 */
public class SQLDimensionProjection {
    @Getter
    private final String columnName;

    @Getter
    private final String tableAlias;

    @Getter
    private final String columnAlias;

    @Getter
    private final Path joinPath;

    /**
     * Constructor.
     *  @param columnName The column alias in SQL to refer to this dimension.
     * @param tableAlias The table alias in SQL where this dimension lives.
     * @param columnAlias The alias to project this column out.
     * @param joinPath A '.' separated path through the entity relationship graph that describe
     *                 how to join the time dimension into the current AnalyticView.
     */
    public SQLDimensionProjection(String columnName, String tableAlias, String columnAlias, Path joinPath) {
        this.columnName = columnName;
        this.tableAlias = tableAlias;
        this.columnAlias = columnAlias;
        this.joinPath = joinPath;
    }

    /**
     * Returns a String that identifies this dimension in a SQL query.
     *
     * @return e.g. <code>table_alias.column_name</code>
     */
    public String getColumnReference() {
        return getTableAlias() + "." + getColumnName();
    }

    /**
     * Build a SQL dimension for a dimension projection.
     *
     * @param projection dimension to project out
     * @param table table that contains this dimension
     * @return constructed dimension
     */
    public static SQLDimensionProjection constructSQLDimension(DimensionProjection projection,
                                                               SQLTable table) {
        String fieldName = projection.getDimension().getName();
        SQLColumn sqlColumn = table.getSQLColumn(fieldName);

        if (projection instanceof TimeDimensionProjection) {
            return new SQLTimeDimensionProjection(
                    sqlColumn.getColumnName(),
                    sqlColumn.getTableAlias(),
                    projection.getAlias(),
                    sqlColumn.getJoinPath(),
                    ((TimeDimensionProjection) projection).getDimension(),
                    ((TimeDimensionProjection) projection).getProjectedGrain());
        }

        return new SQLDimensionProjection(
                sqlColumn.getColumnName(),
                sqlColumn.getTableAlias(),
                projection.getAlias(),
                sqlColumn.getJoinPath());
    }
}
