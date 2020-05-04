/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata.models;

import static com.yahoo.elide.datastores.aggregation.metadata.enums.ColumnType.FIELD;
import static com.yahoo.elide.datastores.aggregation.metadata.enums.ColumnType.FORMULA;
import static com.yahoo.elide.datastores.aggregation.metadata.enums.ColumnType.REFERENCE;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.ToOne;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.datastores.aggregation.annotation.DimensionFormula;
import com.yahoo.elide.datastores.aggregation.annotation.JoinTo;
import com.yahoo.elide.datastores.aggregation.annotation.Meta;
import com.yahoo.elide.datastores.aggregation.annotation.MetricFormula;
import com.yahoo.elide.datastores.aggregation.metadata.enums.ColumnType;
import com.yahoo.elide.datastores.aggregation.metadata.enums.ValueType;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.Id;

/**
 * Column is the super class of a field in a table, it can be either dimension or metric.
 */
@Include(type = "column")
@Getter
@EqualsAndHashCode
@ToString
public abstract class Column {
    @Id
    private final String id;

    private final String name;

    private final String description;

    @ToOne
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private final Table table;

    private final ValueType valueType;

    private final ColumnType columnType;

    private final String expression;

    @ToString.Exclude
    private final Set<String> columnTags;

    protected Column(Table table, String fieldName, EntityDictionary dictionary) {
        this.table = table;
        Class<?> tableClass = dictionary.getEntityClass(table.getName(), table.getVersion());

        this.id = constructColumnName(tableClass, fieldName, dictionary);
        this.name = fieldName;
        this.columnTags = new HashSet<>();

        Meta meta = dictionary.getAttributeOrRelationAnnotation(tableClass, Meta.class, fieldName);
        if (meta != null) {
            this.description = meta.description();
        } else {
            this.description = null;
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
            expression = joinTo.path();
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
