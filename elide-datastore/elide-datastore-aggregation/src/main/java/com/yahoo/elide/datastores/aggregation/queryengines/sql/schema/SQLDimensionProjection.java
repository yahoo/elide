/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql.schema;

import static com.yahoo.elide.datastores.aggregation.AggregationDictionary.getClassAlias;

import com.yahoo.elide.core.Path;
import com.yahoo.elide.datastores.aggregation.AggregationDictionary;
import com.yahoo.elide.datastores.aggregation.metadata.models.Table;
import com.yahoo.elide.datastores.aggregation.query.DimensionProjection;
import com.yahoo.elide.datastores.aggregation.query.TimeDimensionProjection;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.JoinTo;

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
     * @param dimension dimension to project out
     * @param table table that contains this dimension
     * @param dictionary dictionary that manage aggregation store models
     * @return constructed dimension
     */
    public static SQLDimensionProjection constructSQLDimension(
            DimensionProjection dimension,
            Table table,
            AggregationDictionary dictionary) {

        String fieldName = dimension.getDimension().getName();
        Class<?> tableCls = table.getCls();

        JoinTo joinTo = dictionary.getAttributeOrRelationAnnotation(tableCls, JoinTo.class, fieldName);

        if (joinTo == null) {
            String columnName = dictionary.getColumnName(tableCls, fieldName);

            if (dimension instanceof TimeDimensionProjection) {
                return new SQLTimeDimensionProjection(
                        columnName,
                        getClassAlias(tableCls),
                        null,
                        ((TimeDimensionProjection) dimension).getDimension(),
                        ((TimeDimensionProjection) dimension).getProjectedGrain());
            }
            return new SQLDimensionProjection(columnName, getClassAlias(tableCls), dimension.getAlias(), null);
        } else {
            Path path = new Path(tableCls, dictionary, joinTo.path());

            if (dimension instanceof TimeDimensionProjection) {
                return new SQLTimeDimensionProjection(
                        dictionary.getJoinColumn(path),
                        getJoinTableAlias(path),
                        path,
                        ((TimeDimensionProjection) dimension).getDimension(),
                        ((TimeDimensionProjection) dimension).getProjectedGrain()
                );
            }
            return new SQLDimensionProjection(
                    dictionary.getJoinColumn(path), getJoinTableAlias(path), dimension.getAlias(), path);
        }
    }

    private static String getJoinTableAlias(Path path) {
        Path.PathElement last = path.lastElement().get();
        Class<?> lastClass = last.getType();

        return getClassAlias(lastClass);
    }
}
