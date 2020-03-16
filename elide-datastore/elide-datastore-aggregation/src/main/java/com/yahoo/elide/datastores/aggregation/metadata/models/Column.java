/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata.models;

import static com.yahoo.elide.datastores.aggregation.metadata.enums.ColumnType.*;
import static com.yahoo.elide.datastores.aggregation.metadata.enums.ColumnType.FORMULA;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.ToOne;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.Path;
import com.yahoo.elide.datastores.aggregation.annotation.DimensionFormula;
import com.yahoo.elide.datastores.aggregation.annotation.JoinTo;
import com.yahoo.elide.datastores.aggregation.annotation.Meta;
import com.yahoo.elide.datastores.aggregation.annotation.MetricFormula;
import com.yahoo.elide.datastores.aggregation.core.JoinPath;
import com.yahoo.elide.datastores.aggregation.metadata.enums.ColumnType;
import com.yahoo.elide.datastores.aggregation.metadata.enums.ValueType;
import com.yahoo.elide.utils.TypeHelper;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

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

    private ColumnType columnType;

    private String expression;

    @ToString.Exclude
    private Set<String> columnTags;

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

        if (dictionary.attributeOrRelationAnnotationExists(tableClass, fieldName, MetricFormula.class)) {
            columnType = FORMULA;
            expression = dictionary
                    .getAttributeOrRelationAnnotation(tableClass, MetricFormula.class, fieldName).value();
        } else if (dictionary.attributeOrRelationAnnotationExists(tableClass, fieldName, DimensionFormula.class)) {
            columnType = FORMULA;
            expression = dictionary
                    .getAttributeOrRelationAnnotation(tableClass, DimensionFormula.class, fieldName).value();
        } else if (dictionary.attributeOrRelationAnnotationExists(tableClass, fieldName, JoinTo.class)) {
            JoinTo joinTo = dictionary.getAttributeOrRelationAnnotation(tableClass, JoinTo.class, fieldName);

            columnType = REFERENCE;
            JoinPath source = new JoinPath(tableClass, dictionary, joinTo.path());

            Path.PathElement last = source.lastElement().get();
            expression = TypeHelper.getFieldAlias(dictionary.getJsonAliasFor(last.getType()), last.getFieldName());
        } else {
            columnType = FIELD;
            expression = dictionary.getAnnotatedColumnName(tableClass, fieldName);
        }
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
}
