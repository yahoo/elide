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
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.filter.dialect.ParseException;
import com.yahoo.elide.core.filter.dialect.RSQLFilterDialect;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.core.utils.TypeHelper;
import com.yahoo.elide.datastores.aggregation.annotation.CardinalitySize;
import com.yahoo.elide.datastores.aggregation.annotation.TableMeta;
import com.yahoo.elide.datastores.aggregation.annotation.Temporal;
import com.yahoo.elide.datastores.aggregation.metadata.enums.ValueType;
import com.yahoo.elide.datastores.aggregation.query.ColumnProjection;
import com.yahoo.elide.datastores.aggregation.query.Queryable;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromSubquery;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromTable;
import org.apache.commons.lang3.StringUtils;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

/**
 * Super class of all logical or physical tables.
 */
@Include(type = "table")
@Getter
@EqualsAndHashCode
@ToString
public abstract class Table implements Versioned {

    @Id
    private final String id;

    private final String name;

    private final String friendlyName;

    private final String category;

    @Exclude
    private final String version;

    private final String description;

    private final CardinalitySize cardinality;

    private final String requiredFilter;

    private final boolean isFact;

    @ManyToOne
    private final Namespace namespace;

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

    @ToString.Exclude
    @Exclude
    private final Set<String> hints;

    @Exclude
    @ToString.Exclude
    private final Map<String, Column> columnMap;

    @Exclude
    private final String alias;

    @OneToMany
    @ToString.Exclude
    private final Set<Argument> arguments;

    public Table(Namespace namespace, Type<?> cls, EntityDictionary dictionary) {
        if (!dictionary.getBoundClasses().contains(cls)) {
            throw new IllegalArgumentException(
                    String.format("Table class {%s} is not defined in dictionary.", cls));
        }

        this.namespace = namespace;
        namespace.addTable(this);

        this.name = dictionary.getJsonAliasFor(cls);
        this.version = EntityDictionary.getModelVersion(cls);

        this.alias = TypeHelper.getTypeAlias(cls);

        //Namespace is not required in ID for now because table names must be globally unique.
        this.id = this.name;

        TableMeta meta = cls.getAnnotation(TableMeta.class);
        this.isFact = isFact(cls, meta);

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

        if (meta != null) {
            this.friendlyName = meta.friendlyName() != null && !meta.friendlyName().isEmpty()
                    ? meta.friendlyName()
                    : name;
            this.description = meta.description();
            this.category = meta.category();
            this.requiredFilter = meta.filterTemplate();
            this.tags = new HashSet<>(Arrays.asList(meta.tags()));
            this.hints = new LinkedHashSet<>(Arrays.asList(meta.hints()));
            this.cardinality = meta.size();
            if (meta.arguments().length == 0) {
                this.arguments = new HashSet<>();
            } else {
                this.arguments = Arrays.stream(meta.arguments())
                        .map(argument -> new Argument(getId(), argument))
                        .collect(Collectors.toCollection(LinkedHashSet::new));
            }
        } else {
            this.friendlyName = name;
            this.description = null;
            this.category = null;
            this.requiredFilter = null;
            this.tags = new HashSet<>();
            this.hints = new LinkedHashSet<>();
            this.cardinality = CardinalitySize.UNKNOWN;
            this.arguments = new HashSet<>();
        }
    }

    private boolean isFact(Type<?> cls, TableMeta meta) {
        if (meta != null) {
            return meta.isFact();
        }

        // If FromTable or FromSubquery Annotation exists then assume its fact table.
        boolean existsAggAnnotations = (cls.getAnnotation(FromTable.class) != null
                || cls.getAnnotation(FromSubquery.class) != null);

        return existsAggAnnotations;
    }

    /**
     * Construct all columns of this table.
     *
     * @param cls table class
     * @param dictionary dictionary contains the table class
     * @return all resolved column metadata
     */
    private Set<Column> constructColumns(Type<?> cls, EntityDictionary dictionary) {
        Set<Column> columns =  dictionary.getAllFields(cls).stream()
                .filter(field -> {
                    ValueType valueType = getValueType(cls, field, dictionary);
                    return valueType != null && valueType != ValueType.RELATIONSHIP;
                })
                .map(field -> {
                    if (isMetricField(dictionary, cls, field)) {
                        return constructMetric(field, dictionary);
                    }
                    if (dictionary.attributeOrRelationAnnotationExists(cls, field, Temporal.class)) {
                        return constructTimeDimension(field, dictionary);
                    }
                    return constructDimension(field, dictionary);
                })
                .collect(Collectors.toSet());

        // add id field if exists and this is not a fact model
        if (!this.isFact() && dictionary.getIdFieldName(cls) != null) {
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
    protected final <T extends Column> T getColumn(Class<T> cls, String fieldName) {
        Column column = columnMap.get(fieldName);
        return column != null && cls.isAssignableFrom(column.getClass()) ? cls.cast(column) : null;
    }

    /**
     * Returns the metric associated with the given field name.
     * @param fieldName The field to lookup.
     * @return The corresponding metric or null.
     */
    public Metric getMetric(String fieldName) {
        return getColumn(Metric.class, fieldName);
    }

    /**
     * Returns the dimension associated with the given field name.
     * @param fieldName The field to lookup.
     * @return The corresponding dimension or null.
     */
    public Dimension getDimension(String fieldName) {
        return getColumn(Dimension.class, fieldName);
    }

    /**
     * Returns the time dimension associated with the given field name.
     * @param fieldName The field to lookup.
     * @return The corresponding time dimension or null.
     */
    public TimeDimension getTimeDimension(String fieldName) {
        return getColumn(TimeDimension.class, fieldName);
    }

    public boolean isMetric(String fieldName) {
        return getMetric(fieldName) != null;
    }

    public FilterExpression getRequiredFilter(EntityDictionary dictionary) {
        Type<?> cls = dictionary.getEntityClass(name, version);
        RSQLFilterDialect filterDialect = new RSQLFilterDialect(dictionary);

        if (StringUtils.isNotEmpty(requiredFilter)) {
            try {
                return filterDialect.parseFilterExpression(requiredFilter, cls, false, true);
            } catch (ParseException e) {
                throw new IllegalStateException(e);
            }
        }
        return null;
    }

    public ColumnProjection toProjection(Column column) {
        return toQueryable().getColumnProjection(column.getName());
    }

    public abstract Queryable toQueryable();
}
