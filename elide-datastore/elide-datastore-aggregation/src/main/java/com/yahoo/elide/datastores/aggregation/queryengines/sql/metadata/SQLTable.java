/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata;

import static com.yahoo.elide.datastores.aggregation.metadata.ColumnContext.PERIOD;
import static java.util.Collections.emptyMap;

import com.yahoo.elide.core.dictionary.EntityBinding;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.request.Argument;
import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.datastores.aggregation.annotation.Join;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import com.yahoo.elide.datastores.aggregation.metadata.enums.ColumnType;
import com.yahoo.elide.datastores.aggregation.metadata.enums.ValueType;
import com.yahoo.elide.datastores.aggregation.metadata.models.ArgumentDefinition;
import com.yahoo.elide.datastores.aggregation.metadata.models.Column;
import com.yahoo.elide.datastores.aggregation.metadata.models.Dimension;
import com.yahoo.elide.datastores.aggregation.metadata.models.Metric;
import com.yahoo.elide.datastores.aggregation.metadata.models.Namespace;
import com.yahoo.elide.datastores.aggregation.metadata.models.Table;
import com.yahoo.elide.datastores.aggregation.metadata.models.TimeDimension;
import com.yahoo.elide.datastores.aggregation.query.ColumnProjection;
import com.yahoo.elide.datastores.aggregation.query.DimensionProjection;
import com.yahoo.elide.datastores.aggregation.query.MetricProjection;
import com.yahoo.elide.datastores.aggregation.query.MetricProjectionMaker;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.datastores.aggregation.query.Queryable;
import com.yahoo.elide.datastores.aggregation.query.TableSQLMaker;
import com.yahoo.elide.datastores.aggregation.query.TimeDimensionProjection;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.ConnectionDetails;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromSubquery;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromTable;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.query.SQLDimensionProjection;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.query.SQLMetricProjection;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.query.SQLTimeDimensionProjection;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Subselect;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * SQL extension of {@link Table} which also contains sql column meta data.
 */
@EqualsAndHashCode(callSuper = true)
@Getter
public class SQLTable extends Table implements Queryable {

    private ConnectionDetails connectionDetails;

    private Map<String, SQLJoin> joins;

    private Map<String, Argument> arguments;

    private Type<?> cls;

    public SQLTable(Namespace namespace,
                    Type<?> cls,
                    EntityDictionary dictionary,
                    ConnectionDetails connectionDetails) {
        super(namespace, cls, dictionary);
        this.connectionDetails = connectionDetails;
        this.joins = new HashMap<>();
        this.cls = cls;
        this.arguments = prepareArgMap(getArgumentDefinitions());

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

    public SQLTable(Namespace namespace, Type<?> cls, EntityDictionary dictionary) {
        this(namespace, cls, dictionary, null);
    }

    @Override
    protected Metric constructMetric(String fieldName, EntityDictionary dictionary) {
        if (dictionary.getIdFieldName(getModel()).equals(fieldName)) {
            MetricProjectionMaker maker = (metric, alias, arguments) -> {
                return SQLMetricProjection
                        .builder()
                        .projected(true)
                        .alias(fieldName)
                        .name(fieldName)
                        .expression("")
                        .valueType(ValueType.ID)
                        .columnType(ColumnType.FIELD)
                        .arguments(new HashMap<>())
                        .build();

            };
            return new Metric(maker, this, fieldName, dictionary);
        }
        return new Metric(this, fieldName, dictionary);
    }

    @Override
    public Queryable toQueryable() {
        return this;
    }

    @Override
    public MetricProjection getMetricProjection(String fieldName) {
        return getMetricProjection(fieldName, emptyMap());
    }

    public MetricProjection getMetricProjection(String fieldName, String alias) {
        return getMetricProjection(fieldName, alias, emptyMap());
    }

    public MetricProjection getMetricProjection(String fieldName, Map<String, Argument> arguments) {
        return getMetricProjection(fieldName, fieldName, arguments);
    }

    public MetricProjection getMetricProjection(String fieldName, String alias, Map<String, Argument> arguments) {
        Metric metric = super.getMetric(fieldName);
        if (metric == null) {
            return null;
        }

        return getMetricProjection(metric, alias, arguments);
    }

    public MetricProjection getMetricProjection(Metric metric, String alias, Map<String, Argument> arguments) {
        return metric.getMetricProjectionMaker().make(metric, alias, arguments);
    }

    @Override
    public List<MetricProjection> getMetricProjections() {
        return super.getAllMetrics().stream()
                .map(metric -> getMetricProjection(metric, metric.getName(),
                                prepareArgMap(metric.getArgumentDefinitions())))
                .collect(Collectors.toList());
    }

    @Override
    public DimensionProjection getDimensionProjection(String fieldName) {
        return getDimensionProjection(fieldName, emptyMap());
    }

    public DimensionProjection getDimensionProjection(String fieldName, String alias) {
        return getDimensionProjection(fieldName, alias, emptyMap());
    }

    public DimensionProjection getDimensionProjection(String fieldName, Map<String, Argument> arguments) {
        return getDimensionProjection(fieldName, fieldName, arguments);
    }

    public DimensionProjection getDimensionProjection(String fieldName, String alias, Map<String, Argument> arguments) {
        Dimension dimension = super.getDimension(fieldName);
        if (dimension == null) {
            return null;
        }
        return getDimensionProjection(dimension, alias, arguments);
    }

    public DimensionProjection getDimensionProjection(Dimension dimension, String alias,
                    Map<String, Argument> arguments) {
        return new SQLDimensionProjection(dimension, alias, arguments, true);
    }

    @Override
    public List<DimensionProjection> getDimensionProjections() {
        return super.getAllDimensions()
                .stream()
                .map(dimension -> getDimensionProjection(dimension, dimension.getName(),
                                prepareArgMap(dimension.getArgumentDefinitions())))
                .collect(Collectors.toList());
    }

    @Override
    public TimeDimensionProjection getTimeDimensionProjection(String fieldName) {
        return getTimeDimensionProjection(fieldName, new HashMap<>());
    }

    public TimeDimensionProjection getTimeDimensionProjection(String fieldName, Map<String, Argument> arguments) {
        return getTimeDimensionProjection(fieldName, fieldName, arguments);
    }

    public TimeDimensionProjection getTimeDimensionProjection(String fieldName, Set<Argument> arguments) {
        Map<String, Argument> argumentMap =
                arguments.stream().collect(Collectors.toMap(Argument::getName, argument -> argument));
        return getTimeDimensionProjection(fieldName, fieldName, argumentMap);
    }

    public TimeDimensionProjection getTimeDimensionProjection(String fieldName, String alias,
                                                              Map<String, Argument> arguments) {
        TimeDimension dimension = super.getTimeDimension(fieldName);
        if (dimension == null) {
            return null;
        }
        return getTimeDimensionProjection(dimension, alias, arguments);
    }

    public TimeDimensionProjection getTimeDimensionProjection(TimeDimension dimension, String alias,
                    Map<String, Argument> arguments) {
        return new SQLTimeDimensionProjection(dimension, dimension.getTimezone(), alias, arguments, true);
    }

    @Override
    public List<TimeDimensionProjection> getTimeDimensionProjections() {
        return super.getAllTimeDimensions()
                .stream()
                .map(dimension -> getTimeDimensionProjection(dimension, dimension.getName(),
                                prepareArgMap(dimension.getArgumentDefinitions())))
                .collect(Collectors.toList());
    }

    @Override
    public List<ColumnProjection> getColumnProjections() {
        return super.getAllColumns()
                .stream()
                .map(column -> getColumnProjection(column.getName()))
                .collect(Collectors.toList());
    }

    @Override
    public ColumnProjection getColumnProjection(String name) {
        Column column = super.getColumn(Column.class, name);

        if (column == null) {
            return null;
        }

        return getColumnProjection(column, prepareArgMap(column.getArgumentDefinitions()));
    }

    @Override
    public ColumnProjection getColumnProjection(String name, Map<String, Argument> arguments) {
        Column column = super.getColumn(Column.class, name);

        if (column == null) {
            return null;
        }

        return getColumnProjection(column, arguments);
    }

    private ColumnProjection getColumnProjection(Column column, Map<String, Argument> arguments) {
        if (column instanceof TimeDimension) {
            return getTimeDimensionProjection((TimeDimension) column, column.getName(), arguments);
        }

        if (column instanceof Metric) {
            return getMetricProjection((Metric) column, column.getName(), arguments);
        }

        return getDimensionProjection((Dimension) column, column.getName(), arguments);
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

    @Override
    public Map<String, Argument> getArguments() {
        return arguments;
    }

    /**
     * Create a map of String and {@link Argument} using {@link Column}'s arguments.
     * @param arguments Set of available {@link Column} arguments.
     * @return A map of String and {@link Argument}
     */
    private static Map<String, Argument> prepareArgMap(Set<ArgumentDefinition> arguments) {
        return arguments.stream()
                        .map(arg -> Argument.builder()
                                        .name(arg.getName())
                                        .value(arg.getDefaultValue())
                                        .build())
                        .collect(Collectors.toMap(Argument::getName, Function.identity()));
    }

    /**
     * Check whether a class is mapped to a subselect query instead of a physical table.
     * @param cls The entity class.
     * @return True if the class has {@link Subselect}/{@link FromSubquery} annotation.
     */
    public static boolean hasSql(Type<?> cls) {
        return cls.isAnnotationPresent(Subselect.class) || cls.isAnnotationPresent(FromSubquery.class);
    }

    /**
     * Maps an entity class to a physical table or subselect query, if neither {@link jakarta.persistence.Table}
     * nor {@link Subselect} annotation is present on this class, try {@link FromTable} and {@link FromSubquery}.
     * @param cls The entity class.
     * @param clientQuery the client query.
     * @return The physical SQL table or subselect query.
     */
    public static String resolveTableOrSubselect(EntityDictionary dictionary, Type<?> cls, Query clientQuery) {
        if (hasSql(cls)) {
            if (cls.isAnnotationPresent(FromSubquery.class)) {
                FromSubquery fromSubquery = dictionary.getAnnotation(cls, FromSubquery.class);
                if (fromSubquery.maker() != null) {
                    TableSQLMaker maker = dictionary.getInjector().instantiate(fromSubquery.maker());
                    return maker.make(clientQuery);
                }
                return fromSubquery.sql();
            }
            return dictionary.getAnnotation(cls, Subselect.class).value();
        }
        jakarta.persistence.Table table = dictionary.getAnnotation(cls, jakarta.persistence.Table.class);

        if (table != null) {
            return resolveTableAnnotation(table);
        }
        FromTable fromTable = dictionary.getAnnotation(cls, FromTable.class);

        return fromTable != null ? fromTable.name() : cls.getSimpleName();
    }

    /**
     * Get the full table name from JPA {@link jakarta.persistence.Table} annotation.
     * @param table table annotation.
     * @return <code>catalog.schema.name</code>
     */
    private static String resolveTableAnnotation(jakarta.persistence.Table table) {
        StringBuilder fullTableName = new StringBuilder();

        if (StringUtils.isNotBlank(table.catalog())) {
            fullTableName.append(table.catalog()).append(PERIOD);
        }

        if (StringUtils.isNotBlank(table.schema())) {
            fullTableName.append(table.schema()).append(PERIOD);
        }

        return fullTableName.append(table.name()).toString();
    }
}
