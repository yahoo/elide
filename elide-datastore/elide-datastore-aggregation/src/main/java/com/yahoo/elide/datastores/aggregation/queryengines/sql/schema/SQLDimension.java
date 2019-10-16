/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql.schema;

import com.yahoo.elide.core.Path;
import com.yahoo.elide.datastores.aggregation.annotation.CardinalitySize;
import com.yahoo.elide.datastores.aggregation.schema.dimension.Dimension;
import com.yahoo.elide.datastores.aggregation.schema.dimension.DimensionType;

/**
 * A dimension but supporting extra metadata needed to generate SQL.
 */
public class SQLDimension implements Dimension {

    private final Dimension wrapped;
    private final String columnAlias;
    private final String tableAlias;
    private final Path joinPath;

    /**
     * Constructor
     * @param dimension a wrapped dimension.
     * @param columnAlias The column alias in SQL to refer to this dimension.
     * @param tableAlias The table alias in SQL where this dimension lives.
     */
    public SQLDimension(Dimension dimension, String columnAlias, String tableAlias) {
        this(dimension, columnAlias, tableAlias, null);
    }

    /**
     * Constructor
     * @param dimension a wrapped dimension.
     * @param columnAlias The column alias in SQL to refer to this dimension.
     * @param tableAlias The table alias in SQL where this dimension lives.
     * @param joinPath A '.' separated path through the entity relationship graph that describes
     *                 how to join the time dimension into the current AnalyticView.
     */
    public SQLDimension(Dimension dimension, String columnAlias, String tableAlias, Path joinPath) {
        this.wrapped = dimension;
        this.columnAlias = columnAlias;
        this.tableAlias = tableAlias;
        this.joinPath = joinPath;
    }

    @Override
    public String getName() {
        return wrapped.getName();
    }

    @Override
    public String getLongName() {
        return wrapped.getLongName();
    }

    @Override
    public String getDescription() {
        return wrapped.getDescription();
    }

    @Override
    public DimensionType getDimensionType() {
        return wrapped.getDimensionType();
    }

    @Override
    public Class<?> getDataType() {
        return wrapped.getDataType();
    }

    @Override
    public CardinalitySize getCardinality() {
        return wrapped.getCardinality();
    }

    @Override
    public String getFriendlyName() {
        return wrapped.getFriendlyName();
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
     * @return Something like "table_alias.column_name"
     */
    public String getColumnReference() {
        return getTableAlias() + "." + getColumnName();
    }
}
