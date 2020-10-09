/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.datastores.aggregation.metadata.enums.ColumnType;
import com.yahoo.elide.datastores.aggregation.metadata.enums.ValueType;
import com.yahoo.elide.datastores.aggregation.metadata.models.Dimension;
import com.yahoo.elide.datastores.aggregation.metadata.models.Metric;
import com.yahoo.elide.datastores.aggregation.metadata.models.Table;

import com.yahoo.elide.datastores.aggregation.metadata.models.TimeDimension;
import com.yahoo.elide.datastores.aggregation.query.ColumnProjection;
import com.yahoo.elide.datastores.aggregation.query.MetricProjection;
import com.yahoo.elide.datastores.aggregation.query.Queryable;
import com.yahoo.elide.datastores.aggregation.query.TimeDimensionProjection;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.SQLQueryEngine;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.query.SQLColumnProjection;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.query.SQLDimensionProjection;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.query.SQLMetricProjection;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.query.SQLTimeDimensionProjection;
import lombok.EqualsAndHashCode;

import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * SQL extension of {@link Table} which also contains sql column meta data.
 */
@EqualsAndHashCode(callSuper = true)
public class SQLTable extends Table implements Queryable {
    private SQLQueryEngine engine;

    public SQLTable(Class<?> cls, EntityDictionary dictionary, SQLQueryEngine engine) {
        super(cls, dictionary);
        this.engine = engine;
    }

    @Override
    protected SQLMetric constructMetric(String fieldName, EntityDictionary dictionary) {
        return new SQLMetric(this, fieldName, dictionary);
    }

    @Override
    public MetricProjection getMetricProjection(String fieldName) {
        Metric metric = super.getMetric(fieldName);
        if (metric == null) {
            return null;
        }
        return new SQLMetricProjection(metric,
                engine.getReferenceTable(),
                metric.getAlias(),
                new HashMap<>());
    }

    @Override
    public Set<MetricProjection> getMetricProjections() {
        return super.getMetrics().stream()
            .map((metric) ->
                    new SQLMetricProjection(metric,
                            engine.getReferenceTable(),
                            metric.getAlias(),
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
                dimension.getAlias(),
                new HashMap<>(),
                engine.getReferenceTable());
    }

    @Override
    public Set<ColumnProjection> getDimensionProjections() {
        return super.getDimensions()
                .stream()
                .map((dimension) -> new SQLDimensionProjection(dimension,
                        dimension.getAlias(),
                        new HashMap<>(),
                        engine.getReferenceTable()))
                .collect(Collectors.toSet());
    }

    @Override
    public TimeDimensionProjection getTimeDimensionProjection(String fieldName) {
        TimeDimension dimension = super.getTimeDimension(fieldName);
        if (dimension == null) {
            return null;
        }
        return new SQLTimeDimensionProjection(dimension,
                dimension.getTimeZone(),
                engine.getReferenceTable(),
                dimension.getAlias(),
                new HashMap<>());
    }

    @Override
    public Set<TimeDimensionProjection> getTimeDimensionProjections() {
        return super.getTimeDimensions()
                .stream()
                .map((dimension) -> new SQLTimeDimensionProjection(dimension,
                        dimension.getTimeZone(),
                        engine.getReferenceTable(),
                        dimension.getAlias(),
                        new HashMap<>()))
                .collect(Collectors.toSet());
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
                return engine.getReferenceTable();
            }

            @Override
            public Queryable getSource() {
                return SQLTable.this;
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
        };
    }
}
