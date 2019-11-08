/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata.models;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.datastores.aggregation.AggregationDictionary;
import com.yahoo.elide.datastores.aggregation.annotation.Cardinality;
import com.yahoo.elide.datastores.aggregation.annotation.CardinalitySize;
import com.yahoo.elide.datastores.aggregation.annotation.Meta;
import com.yahoo.elide.datastores.aggregation.annotation.Temporal;

import lombok.Data;
import lombok.ToString;

import java.util.Set;
import java.util.stream.Collectors;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Transient;

/**
 * Super class of all logical or physical tables
 */
@Include(rootLevel = true, type = "table")
@Entity
@Data
@ToString
public class Table {
    @Transient
    private Class<?> cls;

    @Id
    private String name;

    private String longName;

    private String description;

    private String category;

    private CardinalitySize cardinality;

    @OneToMany
    @ToString.Exclude
    private Set<Column> columns;

    public Table(Class<?> cls, AggregationDictionary dictionary) {
        if (!dictionary.getBindings().contains(cls)) {
            throw new IllegalArgumentException(
                    String.format("Table class {%s} is not defined in dictionary.", cls));
        }

        this.cls = cls;
        this.name = dictionary.getJsonAliasFor(cls);
        this.columns = resolveColumns(cls, dictionary);

        Meta meta = cls.getAnnotation(Meta.class);
        if (meta != null) {
            this.longName = meta.longName();
            this.description = meta.description();
        }

        Cardinality cardinality = dictionary.getAnnotation(cls, Cardinality.class);
        if (cardinality != null) {
            this.cardinality = cardinality.size();
        }
    }

    /**
     * Add all columns of this table.
     *
     * @param cls table class
     * @param dictionary dictionary contains the table class
     * @return all resolved column metadata
     */
    private static Set<Column> resolveColumns(Class<?> cls, AggregationDictionary dictionary) {
        Set<Column> fields =  dictionary.getAllFields(cls).stream()
                .map(field -> {
                    if (dictionary.isMetricField(cls, field)) {
                        return new Metric(cls, field, dictionary);
                    } else if (dictionary.attributeOrRelationAnnotationExists(cls, field, Temporal.class)) {
                        return new TimeDimension(cls, field, dictionary);
                    } else {
                        return new Dimension(cls, field, dictionary);
                    }
                })
                .collect(Collectors.toSet());

        // add id field
        fields.add(new Dimension(cls, dictionary.getIdFieldName(cls), dictionary));

        return fields;
    }

    /**
     * Get all columns of a specific class, can be {@link Metric}, {@link TimeDimension} or {@link Dimension}
     *
     * @param cls metadata class
     * @param <T> metadata class
     * @return columns as requested type if found
     */
    public <T extends Column> Set<T> getColumns(Class<T> cls) {
        return columns.stream()
                .filter(col -> cls.isAssignableFrom(col.getClass()))
                .map(cls::cast)
                .collect(Collectors.toSet());
    }

    /**
     * Get a field column as a specific class, can be {@link Metric}, {@link TimeDimension} or {@link Dimension}
     *
     * @param cls metadata class
     * @param fieldName logical column name
     * @param <T> metadata class
     * @return column as requested type if found
     */
    private <T extends Column> T getColumn(Class<T> cls, String fieldName) {
        return columns.stream()
                .filter(col -> cls.isAssignableFrom(col.getClass()) && (col.getName().equals(fieldName)))
                .map(cls::cast)
                .findFirst()
                .orElse(null);
    }

    public Metric getMetric(String fieldName) {
        return getColumn(Metric.class, fieldName);
    }

    public Dimension getDimension(String fieldName) {
        return getColumn(Dimension.class, fieldName);
    }

    public TimeDimension getTimeDimension(String fieldName) {
        return getColumn(TimeDimension.class, fieldName);
    }

    public boolean isMetric(String fieldName) {
        return getMetric(fieldName) != null;
    }
}
