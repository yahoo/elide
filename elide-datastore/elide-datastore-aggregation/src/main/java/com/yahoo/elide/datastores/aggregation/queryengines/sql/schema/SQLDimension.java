/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql.schema;

import com.yahoo.elide.core.Path;
import com.yahoo.elide.datastores.aggregation.AggregationDictionary;
import com.yahoo.elide.datastores.aggregation.metadata.models.Table;
import com.yahoo.elide.datastores.aggregation.query.DimensionProjection;
import com.yahoo.elide.datastores.aggregation.query.TimeDimensionProjection;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.SQLQueryEngine;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.JoinTo;

/**
 * Physical Dimension in a sql table.
 */
public class SQLDimension {
    private final String columnAlias;
    private final String tableAlias;
    private final Path joinPath;

    /**
     * Constructor.
     *
     * @param columnAlias The column alias in SQL to refer to this dimension.
     * @param tableAlias The table alias in SQL where this dimension lives.
     */
    public SQLDimension(String columnAlias, String tableAlias, Path joinPath) {
        this.columnAlias = columnAlias;
        this.tableAlias = tableAlias;
        this.joinPath = joinPath;
    }

    public String getColumnName() {
        return columnAlias;
    }

    public String getTableAlias() {
        return tableAlias;
    }

    /**
     * Returns the join path of this dimension.
     * @return Something like "author.book.publisher.name"
     */
    public Path getJoinPath() {
        return joinPath;
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
     * @param dimension dimension to project out
     * @param table table that contains this dimension
     * @param dictionary dictionary that manage aggregation store models
     * @return constructed dimension
     */
    public static SQLDimension constructSQLDimension(
            DimensionProjection dimension,
            Table table,
            AggregationDictionary dictionary) {

        String fieldName = dimension.getName();
        Class<?> tableCls = table.getCls();

        JoinTo joinTo = dictionary.getAttributeOrRelationAnnotation(tableCls, JoinTo.class, fieldName);

        if (joinTo == null) {
            String columnName = dictionary.getColumnName(tableCls, fieldName);

            if (dimension instanceof TimeDimensionProjection) {
                return new SQLTimeDimension(
                        columnName,
                        SQLQueryEngine.getClassAlias(tableCls),
                        null);
            }
            return new SQLDimension(columnName, SQLQueryEngine.getClassAlias(tableCls), null);
        } else {
            Path path = new Path(tableCls, dictionary, joinTo.path());

            if (dimension instanceof TimeDimensionProjection) {
                return new SQLTimeDimension(
                        dictionary.getJoinColumn(path),
                        getJoinTableAlias(path),
                        path
                );
            }
            return new SQLDimension(dictionary.getJoinColumn(path), getJoinTableAlias(path), path);
        }
    }

    private static String getJoinTableAlias(Path path) {
        Path.PathElement last = path.lastElement().get();
        Class<?> lastClass = last.getType();

        return SQLQueryEngine.getClassAlias(lastClass);
    }
}
