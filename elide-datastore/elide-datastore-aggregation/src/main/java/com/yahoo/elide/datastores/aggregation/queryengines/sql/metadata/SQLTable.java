/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata;

import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.datastores.aggregation.metadata.enums.ColumnType;
import com.yahoo.elide.datastores.aggregation.metadata.enums.ValueType;
import com.yahoo.elide.datastores.aggregation.metadata.models.Column;
import com.yahoo.elide.datastores.aggregation.metadata.models.Dimension;
import com.yahoo.elide.datastores.aggregation.metadata.models.Metric;
import com.yahoo.elide.datastores.aggregation.metadata.models.Table;
import com.yahoo.elide.datastores.aggregation.metadata.models.TimeDimension;
import com.yahoo.elide.datastores.aggregation.query.ColumnProjection;
import com.yahoo.elide.datastores.aggregation.query.MetricProjection;
import com.yahoo.elide.datastores.aggregation.query.Queryable;
import com.yahoo.elide.datastores.aggregation.query.TimeDimensionProjection;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.ConnectionDetails;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.query.SQLColumnProjection;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.query.SQLDimensionProjection;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.query.SQLMetricProjection;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.query.SQLTimeDimensionProjection;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * SQL extension of {@link Table} which also contains sql column meta data.
 */
@EqualsAndHashCode(callSuper = true)
public class SQLTable extends Table implements Queryable {

    @Getter
    private ConnectionDetails connectionDetails;

    public SQLTable(Type<?> cls,
                    EntityDictionary dictionary,
                    ConnectionDetails connectionDetails) {
        super(cls, dictionary);
        this.connectionDetails = connectionDetails;
    }

    public SQLTable(Type<?> cls, EntityDictionary dictionary) {
        super(cls, dictionary);
    }

    @Override
    protected Metric constructMetric(String fieldName, EntityDictionary dictionary) {
        return new Metric(this, fieldName, dictionary);
    }

    @Override
    public ColumnProjection toProjection(Column column) {
        ColumnProjection projection;
        projection = getTimeDimensionProjection(column.getName());
        if (projection != null) {
            return projection;
        }
        projection = getMetricProjection(column.getName());
        if (projection != null) {
            return projection;
        }
        return getColumnProjection(column.getName());
    }

    @Override
    public Queryable toQueryable() {
        return this;
    }

    @Override
    public MetricProjection getMetricProjection(String fieldName) {
        Metric metric = super.getMetric(fieldName);
        if (metric == null) {
            return null;
        }
        return new SQLMetricProjection(metric,
                metric.getName(),
                new HashMap<>());
    }

    @Override
    public Set<MetricProjection> getMetricProjections() {
        return super.getMetrics().stream()
                .map((metric) ->
                        new SQLMetricProjection(metric,
                                metric.getName(),
                                new HashMap<>()))
                .collect(Collectors.toSet());
    }

    @Override
    public ColumnProjection getDimensionProjection(String fieldName) {
        Dimension dimension = super.getDimension(fieldName);
        if (dimension == null) {
            return null;
        }
        return new SQLDimensionProjection(dimension,
                dimension.getName(),
                new HashMap<>());

    }

    @Override
    public Set<ColumnProjection> getDimensionProjections() {
        return super.getDimensions()
                .stream()
                .map((dimension) -> new SQLDimensionProjection(dimension,
                        dimension.getName(),
                        new HashMap<>()))
                .collect(Collectors.toSet());
    }

    @Override
    public TimeDimensionProjection getTimeDimensionProjection(String fieldName) {
        TimeDimension dimension = super.getTimeDimension(fieldName);
        if (dimension == null) {
            return null;
        }
        return new SQLTimeDimensionProjection(dimension,
                dimension.getTimezone(),
                dimension.getName(),
                new HashMap<>());
    }

    @Override
    public Set<TimeDimensionProjection> getTimeDimensionProjections() {
        return super.getTimeDimensions()
                .stream()
                .map((dimension) -> new SQLTimeDimensionProjection(dimension,
                        dimension.getTimezone(),
                        dimension.getName(),
                        new HashMap<>()))
                .collect(Collectors.toSet());
    }

    @Override
    public Set<ColumnProjection> getColumnProjections() {
        return super.getColumns()
                .stream()
                .map((column) -> { return getColumnProjection(column.getName()); })
                .collect(Collectors.toSet());
    }

    @Override
    public String getAlias(String columnName) {
        return super.getAlias();
    }

    @Override
    public ColumnProjection getColumnProjection(String name) {
        Column column = super.getColumn(Column.class, name);

        if (column == null) {
            return null;
        }

        if (column instanceof TimeDimension) {
            return getTimeDimensionProjection(name);
        }

        return new SQLColumnProjection() {

            @Override
            public Queryable getSource() {
                return SQLTable.this;
            }

            @Override
            public String getAlias() {
                return column.getName();
            }

            @Override
            public String getId() {
                return column.getId();
            }

            @Override
            public String getName() {
                return column.getName();
            }

            @Override
            public String getExpression() {
                return column.getExpression();
            }

            @Override
            public ValueType getValueType() {
                return column.getValueType();
            }

            @Override
            public ColumnType getColumnType() {
                return column.getColumnType();
            }
        };
    }

    @Override
    public Queryable getSource() {
        return this;
    }
}
