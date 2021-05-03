/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata;

import static org.apache.commons.lang3.StringUtils.isBlank;
import com.yahoo.elide.core.dictionary.EntityBinding;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.request.Argument;
import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.datastores.aggregation.annotation.Join;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import com.yahoo.elide.datastores.aggregation.metadata.models.Column;
import com.yahoo.elide.datastores.aggregation.metadata.models.Dimension;
import com.yahoo.elide.datastores.aggregation.metadata.models.Metric;
import com.yahoo.elide.datastores.aggregation.metadata.models.Table;
import com.yahoo.elide.datastores.aggregation.metadata.models.TimeDimension;
import com.yahoo.elide.datastores.aggregation.query.ColumnProjection;
import com.yahoo.elide.datastores.aggregation.query.DimensionProjection;
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
@Getter
public class SQLTable extends Table implements Queryable {

    private ConnectionDetails connectionDetails;

    private Map<String, SQLJoin> joins;

    public SQLTable(Type<?> cls,
                    EntityDictionary dictionary,
                    ConnectionDetails connectionDetails) {
        super(cls, dictionary);
        this.connectionDetails = connectionDetails;
        this.joins = new HashMap<>();

        EntityBinding binding = dictionary.getEntityBinding(cls);
        binding.fieldsToValues.forEach((name, field) -> {
            if (field.isAnnotationPresent(Join.class)) {
                Join join = field.getAnnotation(Join.class);
                joins.put(name, SQLJoin.builder()
                        .name(name)
                        .joinType(join.type())
                        .joinExpression(join.value())
                        .joinTableType(dictionary.getParameterizedType(cls, name))
                        .toOne(join.toOne())
                        .build());
            }
        });
    }

    public SQLTable(Type<?> cls, EntityDictionary dictionary) {
        this(cls, dictionary, null);
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
        return getMetricProjection(fieldName, alias, new HashMap<>());
    }

    public MetricProjection getMetricProjection(String fieldName, String alias, Map<String, Argument> arguments) {
        Metric metric = super.getMetric(fieldName);
        if (metric == null) {
            return null;
        }

        return metric.getMetricProjectionMaker().make(metric,
                isBlank(alias) ? metric.getName() : alias,
                arguments);
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
    public DimensionProjection getDimensionProjection(String fieldName) {
        return getDimensionProjection(fieldName, null);
    }

    public DimensionProjection getDimensionProjection(String fieldName, String alias) {
        return getDimensionProjection(fieldName, alias, new HashMap<>());
    }

    public DimensionProjection getDimensionProjection(String fieldName, String alias, Map<String, Argument> arguments) {
        Dimension dimension = super.getDimension(fieldName);
        if (dimension == null) {
            return null;
        }
        return new SQLDimensionProjection(dimension,
                isBlank(alias) ? dimension.getName() : alias,
                arguments, true);
    }

    @Override
    public List<DimensionProjection> getDimensionProjections() {
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

    /**
     * Looks up a join by name.
     * @param joinName The join name.
     * @return SQLJoin or null.
     */
    public SQLJoin getJoin(String joinName) {
        return joins.get(joinName);
    }

    /**
     * Looks up a join by name and returns the corresponding SQLTable being joined to.
     * @param store The store where all the SQL tables live.
     * @param joinName The join to lookup.
     * @return SQLTable or null.
     */
    public SQLTable getJoinTable(MetaDataStore store, String joinName) {
        SQLJoin join = getJoin(joinName);
        if (join == null) {
            return null;
        }

        return store.getTable(join.getJoinTableType());
    }

    /**
     * Looks up to see if a given field is a join field or just an attribute.
     * @param store The metadata store.
     * @param modelType The model type in question.
     * @param fieldName The field name in question.
     * @return True if the field is a join field.  False otherwise.
     */
    public static boolean isTableJoin(MetaDataStore store, Type<?> modelType, String fieldName) {
        SQLTable table = store.getTable(modelType);

        return (table.getJoinTable(store, fieldName) != null);
    }

    @Override
    public Queryable getSource() {
        return this;
    }
}
