/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata.models;

import static com.yahoo.elide.utils.TypeHelper.getFieldAlias;

import com.yahoo.elide.annotation.Exclude;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.ToOne;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.Path;
import com.yahoo.elide.datastores.aggregation.annotation.Meta;
import com.yahoo.elide.datastores.aggregation.metadata.LabelResolver;
import com.yahoo.elide.datastores.aggregation.metadata.LabelStore;
import com.yahoo.elide.datastores.aggregation.metadata.enums.ValueType;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.Id;

/**
 * Column is the super class of a field in a table, it can be either dimension or metric.
 */
@Include(type = "column")
@Data
@ToString
public abstract class Column {
    @Id
    private String id;

    private String name;

    private String longName;

    private String description;

    private String category;

    @ToOne
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Table table;

    private ValueType valueType;

    @ToOne
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Column sourceColumn;

    @ToString.Exclude
    private Set<String> columnTags;

    @Exclude
    private LabelResolver labelResolver;

    protected Column(Table table, String fieldName, EntityDictionary dictionary) {
        this.table = table;
        Class<?> tableClass = dictionary.getEntityClass(table.getId());

        this.id = constructColumnName(tableClass, fieldName, dictionary);
        this.name = fieldName;
        this.columnTags = new HashSet<>();

        Meta meta = dictionary.getAttributeOrRelationAnnotation(tableClass, Meta.class, fieldName);
        if (meta != null) {
            this.longName = meta.longName();
            this.description = meta.description();
        }

        valueType = getValueType(tableClass, fieldName, dictionary);
        if (valueType == null) {
            throw new IllegalArgumentException("Unknown data type for " + this.id);
        }

        this.labelResolver = constructLabelResolver(dictionary);
    }

    /**
     * Construct a column name as meta data
     *
     * @param tableClass table class
     * @param fieldName field name
     * @param dictionary entity dictionary to use
     * @return <code>tableAlias.fieldName</code>
     */
    protected static String constructColumnName(Class<?> tableClass, String fieldName, EntityDictionary dictionary) {
        return dictionary.getJsonAliasFor(tableClass) + "." + fieldName;
    }

    /**
     * Resolve the value type of a field
     *
     * @param tableClass table class
     * @param fieldName field name
     * @param dictionary meta data dictionary
     * @return field value type
     */
    public static ValueType getValueType(Class<?> tableClass, String fieldName, EntityDictionary dictionary) {
        if (dictionary.isRelation(tableClass, fieldName)) {
            return ValueType.RELATIONSHIP;
        } else {
            Class<?> fieldClass = dictionary.getType(tableClass, fieldName);

            if (fieldName.equals(dictionary.getIdFieldName(tableClass))) {
                return ValueType.ID;
            } else if (Date.class.isAssignableFrom(fieldClass)) {
                return ValueType.TIME;
            } else {
                return ValueType.getScalarType(fieldClass);
            }
        }
    }

    /**
     * Return a Path that navigate to the source of this column.
     *
     * @param metadataDictionary metadata dictionary
     * @return Path to source column
     */
    public Path getSourcePath(EntityDictionary metadataDictionary) {
        Class<?> tableCls = metadataDictionary.getEntityClass(table.getId());
        Class<?> columnCls = metadataDictionary.getParameterizedType(tableCls, getName());

        return new Path(Collections.singletonList(new Path.PathElement(tableCls, columnCls, getName())));
    }

    /**
     * Construct a label resolver for this column.
     *
     * @return a label resolver
     */
    protected LabelResolver constructLabelResolver(EntityDictionary dictionary) {
        return new LabelResolver(this) {
            @Override
            public String resolveLabel(LabelStore labelStore, String tableAlias) {
                return getFieldAlias(tableAlias, getName());
            }
        };
    }

    /**
     * Resolve physical reference label of this column.
     *
     * @param labelStore table stores all resolvers
     */
    public void resolveLabel(LabelStore labelStore) {
        // NOOP
    }
}
