/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata.models;

import static com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore.isMetricField;
import static com.yahoo.elide.datastores.aggregation.metadata.models.Column.getValueType;

import com.yahoo.elide.annotation.Exclude;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.filter.dialect.ParseException;
import com.yahoo.elide.core.filter.dialect.RSQLFilterDialect;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.datastores.aggregation.annotation.Cardinality;
import com.yahoo.elide.datastores.aggregation.annotation.CardinalitySize;
import com.yahoo.elide.datastores.aggregation.annotation.TableMeta;
import com.yahoo.elide.datastores.aggregation.annotation.Temporal;
import com.yahoo.elide.datastores.aggregation.query.ColumnProjection;
import com.yahoo.elide.datastores.aggregation.query.MetricProjection;
import com.yahoo.elide.datastores.aggregation.query.QueryVisitor;
import com.yahoo.elide.datastores.aggregation.query.Queryable;
import com.yahoo.elide.datastores.aggregation.query.TimeDimensionProjection;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromSubquery;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromTable;

import com.yahoo.elide.utils.TypeHelper;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.persistence.Id;
import javax.persistence.OneToMany;

/**
 * Super class of all logical or physical tables.
 */
@Include(type = "table")
@Getter
@EqualsAndHashCode
@ToString
public class Table implements Queryable, Versioned  {

    @Id
    private final String id;

    private final String name;

    private final String dbConnectionName;

    private final String category;

    @Exclude
    private final String version;

    private final String description;

    private final CardinalitySize cardinality;

    private final String requiredFilter;

    @OneToMany
    @ToString.Exclude
    private final Set<Column> columns;

    @OneToMany
    @ToString.Exclude
    private final Set<Metric> metrics;

    @OneToMany
    @ToString.Exclude
    private final Set<Dimension> dimensions;

    @OneToMany
    @ToString.Exclude
    private final Set<TimeDimension> timeDimensions;

    @ToString.Exclude
    private final Set<String> tags;

    @Exclude
    @ToString.Exclude
    private final Map<String, Column> columnMap;

    @Exclude
    private final String alias;

    public Table(Class<?> cls, EntityDictionary dictionary) {
        if (!dictionary.getBoundClasses().contains(cls)) {
            throw new IllegalArgumentException(
                    String.format("Table class {%s} is not defined in dictionary.", cls));
        }

        this.name = dictionary.getJsonAliasFor(cls);
        this.version = EntityDictionary.getModelVersion(cls);

        this.alias = TypeHelper.getTypeAlias(cls);

        String dbConnectionName = "";
        Annotation annotation =
                        EntityDictionary.getFirstAnnotation(cls, Arrays.asList(FromTable.class, FromSubquery.class));
        if (annotation instanceof FromTable) {
            dbConnectionName = ((FromTable) annotation).dbConnectionName();
        } else if (annotation instanceof FromSubquery) {
            dbConnectionName = ((FromSubquery) annotation).dbConnectionName();
        }

        this.id = this.name;
        this.dbConnectionName = dbConnectionName;

        this.columns = constructColumns(cls, dictionary);
        this.columnMap = this.columns.stream().collect(Collectors.toMap(Column::getName, Function.identity()));

        this.metrics = this.columns.stream()
                .filter(col -> col instanceof Metric)
                .map(Metric.class::cast)
                .collect(Collectors.toSet());
        this.dimensions = this.columns.stream()
                .filter(col -> !(col instanceof Metric || col instanceof TimeDimension))
                .map(Dimension.class::cast)
                .collect(Collectors.toSet());
        this.timeDimensions = this.columns.stream()
                .filter(col -> (col instanceof TimeDimension))
                .map(TimeDimension.class::cast)
                .collect(Collectors.toSet());

        TableMeta meta = cls.getAnnotation(TableMeta.class);

        if (meta != null) {
            this.description = meta.description();
            this.category = meta.category();
            this.requiredFilter = meta.filterTemplate();
            this.tags = new HashSet<>(Arrays.asList(meta.tags()));
        } else {
            this.description = null;
            this.category = null;
            this.requiredFilter = null;
            this.tags = new HashSet<>();
        }

        Cardinality cardinality = dictionary.getAnnotation(cls, Cardinality.class);
        if (cardinality != null) {
            this.cardinality = cardinality.size();
        } else {
            this.cardinality = null;
        }
    }

    /**
     * Construct all columns of this table.
     *
     * @param cls table class
     * @param dictionary dictionary contains the table class
     * @return all resolved column metadata
     */
    private Set<Column> constructColumns(Class<?> cls, EntityDictionary dictionary) {
        Set<Column> columns =  dictionary.getAllFields(cls).stream()
                .filter(field -> getValueType(cls, field, dictionary) != null)
                .map(field -> {
                    if (isMetricField(dictionary, cls, field)) {
                        return constructMetric(field, dictionary);
                    } else if (dictionary.attributeOrRelationAnnotationExists(cls, field, Temporal.class)) {
                        return constructTimeDimension(field, dictionary);
                    } else {
                        return constructDimension(field, dictionary);
                    }
                })
                .collect(Collectors.toSet());

        // add id field if exists
        if (dictionary.getIdFieldName(cls) != null) {
            columns.add(constructDimension(dictionary.getIdFieldName(cls), dictionary));
        }

        return columns;
    }

    /**
     * Construct a Metric instance.
     *
     * @param fieldName field name
     * @param dictionary dictionary contains the table class
     * @return Metric metadata instance
     */
    protected Metric constructMetric(String fieldName, EntityDictionary dictionary) {
        return new Metric(this, fieldName, dictionary);
    }

    /**
     * Construct a Dimension instance.
     *
     * @param fieldName field name
     * @param dictionary dictionary contains the table class
     * @return Dimension metadata instance
     */
    protected TimeDimension constructTimeDimension(String fieldName, EntityDictionary dictionary) {
        return new TimeDimension(this, fieldName, dictionary);
    }

    /**
     * Construct a TimeDimension instance.
     *
     * @param fieldName field name
     * @param dictionary dictionary contains the table class
     * @return TimeDimension metadata instance
     */
    protected Dimension constructDimension(String fieldName, EntityDictionary dictionary) {
        return new Dimension(this, fieldName, dictionary);
    }

    /**
     * Get all columns of a specific class, can be {@link Metric}, {@link TimeDimension} or {@link Dimension}.
     *
     * @param cls metadata class
     * @param <T> metadata class
     * @return columns as requested type if found
     */
    public <T extends ColumnProjection> Set<T> getColumns(Class<T> cls) {
        return columns.stream()
                .filter(col -> cls.isAssignableFrom(col.getClass()))
                .map(cls::cast)
                .collect(Collectors.toSet());
    }

    /**
     * Get a field column as a specific class, can be {@link Metric}, {@link TimeDimension} or {@link Dimension}.
     *
     * @param cls metadata class
     * @param fieldName logical column name
     * @param <T> metadata class
     * @return column as requested type if found
     */
    protected final <T extends ColumnProjection> T getColumn(Class<T> cls, String fieldName) {
        Column column = columnMap.get(fieldName);
        return column != null && cls.isAssignableFrom(column.getClass()) ? cls.cast(column) : null;
    }

    public Metric getMetricProjection(String fieldName) {
        return getColumn(Metric.class, fieldName);
    }

    @Override
    public Set<MetricProjection> getMetricProjections() {
        return metrics.stream().map(MetricProjection.class::cast).collect(Collectors.toSet());
    }

    @Override
    public ColumnProjection getColumnProjection(String name) {
        return columnMap.get(name);
    }

    public Dimension getDimensionProjection(String fieldName) {
        return getColumn(Dimension.class, fieldName);
    }

    @Override
    public Set<ColumnProjection> getDimensionProjections() {
        return metrics.stream().map(ColumnProjection.class::cast).collect(Collectors.toSet());
    }

    public TimeDimensionProjection getTimeDimensionProjection(String fieldName) {
        return getColumn(TimeDimensionProjection.class, fieldName);
    }

    @Override
    public Set<TimeDimensionProjection> getTimeDimensionProjections() {
        return metrics.stream().map(TimeDimensionProjection.class::cast).collect(Collectors.toSet());
    }

    @Override
    public Set<ColumnProjection> getColumnProjections() {
        return getColumns().stream().map(ColumnProjection.class::cast).collect(Collectors.toSet());
    }

    public boolean isMetric(String fieldName) {
        return getMetricProjection(fieldName) != null;
    }

    public FilterExpression getRequiredFilter(EntityDictionary dictionary) {
        Class<?> cls = dictionary.getEntityClass(name, version);
        RSQLFilterDialect filterDialect = new RSQLFilterDialect(dictionary);

        if (requiredFilter != null && !requiredFilter.isEmpty()) {
            try {
                return filterDialect.parseFilterExpression(requiredFilter, cls, false, true);
            } catch (ParseException e) {
                throw new IllegalStateException(e);
            }
        }
        return null;
    }

    @Override
    public <T> T accept(QueryVisitor<T> visitor) {
        return visitor.visitTable(this);
    }
}
