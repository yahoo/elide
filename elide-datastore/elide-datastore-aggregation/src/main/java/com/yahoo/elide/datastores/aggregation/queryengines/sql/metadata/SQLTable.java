/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata;

import static org.apache.commons.lang3.StringUtils.isBlank;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.request.Argument;
import com.yahoo.elide.core.type.Type;
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
import com.yahoo.elide.datastores.aggregation.queryengines.sql.query.SQLDimensionProjection;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.query.SQLMetricProjection;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.query.SQLTimeDimensionProjection;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        return getMetricProjection(fieldName, null);
    }

    public MetricProjection getMetricProjection(String fieldName, String alias) {
        Metric metric = super.getMetric(fieldName);
        if (metric == null) {
            return null;
        }

        return metric.getMetricProjectionMaker().make(metric,
                isBlank(alias) ? metric.getName() : alias,
                new HashMap<>());
    }

    @Override
    public List<MetricProjection> getMetricProjections() {
        return super.getMetrics().stream()
                .map((metric) ->
                        new SQLMetricProjection(metric,
                                metric.getName(),
                                new HashMap<>()))
                .collect(Collectors.toList());
    }

    @Override
    public ColumnProjection getDimensionProjection(String fieldName) {
        return getDimensionProjection(fieldName, null);
    }

    public ColumnProjection getDimensionProjection(String fieldName, String alias) {
        Dimension dimension = super.getDimension(fieldName);
        if (dimension == null) {
            return null;
        }
        return new SQLDimensionProjection(dimension,
                isBlank(alias) ? dimension.getName() : alias,
                new HashMap<>(), true);
    }

    @Override
    public List<ColumnProjection> getDimensionProjections() {
        return super.getDimensions()
                .stream()
                .map((dimension) -> new SQLDimensionProjection(dimension,
                        dimension.getName(),
                        new HashMap<>(), true))
                .collect(Collectors.toList());
    }

    @Override
    public TimeDimensionProjection getTimeDimensionProjection(String fieldName) {
        return getTimeDimensionProjection(fieldName, new HashMap<>());
    }

    public TimeDimensionProjection getTimeDimensionProjection(String fieldName, Map<String, Argument> arguments) {
        return getTimeDimensionProjection(fieldName, null, arguments);
    }

    public TimeDimensionProjection getTimeDimensionProjection(String fieldName, Set<Argument> arguments) {
        Map<String, Argument> argumentMap =
                arguments.stream().collect(Collectors.toMap(Argument::getName, argument -> argument));
        return getTimeDimensionProjection(fieldName, null, argumentMap);
    }

    public TimeDimensionProjection getTimeDimensionProjection(String fieldName, String alias,
                                                              Map<String, Argument> arguments) {
        TimeDimension dimension = super.getTimeDimension(fieldName);
        if (dimension == null) {
            return null;
        }
        return new SQLTimeDimensionProjection(dimension,
                dimension.getTimezone(),
                isBlank(alias) ? dimension.getName() : alias,
                arguments, true);
    }

    @Override
    public List<TimeDimensionProjection> getTimeDimensionProjections() {
        return super.getTimeDimensions()
                .stream()
                .map((dimension) -> new SQLTimeDimensionProjection(dimension,
                        dimension.getTimezone(),
                        dimension.getName(),
                        new HashMap<>(), true))
                .collect(Collectors.toList());
    }

    @Override
    public List<ColumnProjection> getColumnProjections() {
        return super.getColumns()
                .stream()
                .map(column -> getColumnProjection(column.getName()))
                .collect(Collectors.toList());
    }

    @Override
    public ColumnProjection getColumnProjection(String name) {
        return getColumnProjection(name, Collections.emptyMap());
    }

    @Override
    public ColumnProjection getColumnProjection(String name, Map<String, Argument> arguments) {
        Column column = super.getColumn(Column.class, name);

        if (column == null) {
            return null;
        }

        if (column instanceof TimeDimension) {
            return getTimeDimensionProjection(name, arguments);
        }

        if (column instanceof Metric) {
            return getMetricProjection(name);
        }

        return getDimensionProjection(name);
    }

    @Override
    public Queryable getSource() {
        return this;
    }
}
