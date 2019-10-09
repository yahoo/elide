/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.engine.schema;

import com.yahoo.elide.core.Path;
import com.yahoo.elide.datastores.aggregation.annotation.CardinalitySize;
import com.yahoo.elide.datastores.aggregation.dimension.Dimension;
import com.yahoo.elide.datastores.aggregation.dimension.DimensionType;

public class SQLDimension implements Dimension {

    private final Dimension wrapped;
    private final String columnAlias;
    private final String tableAlias;
    private final Path joinPath;

    public SQLDimension(Dimension dimension, String columnAlias, String tableAlias) {
        this(dimension, columnAlias, tableAlias, null);
    }

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

    public Path getJoinPath() {
        return joinPath;
    }

    public String getColumnReference() {
        return getTableAlias() + "." + getColumnName();
    }
}
