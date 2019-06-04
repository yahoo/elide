/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata.models;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.datastores.aggregation.annotation.Meta;
import com.yahoo.elide.datastores.aggregation.metadata.enums.Tag;
import com.yahoo.elide.datastores.aggregation.metadata.enums.ValueType;

import lombok.Data;
import lombok.ToString;

import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

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

    private String tableName;

    private String description;

    private String category;

    @ManyToOne
    private DataType dataType;

    @ToString.Exclude
    private Set<Tag> columnTags;

    protected Column(Class<?> tableClass, String fieldName, EntityDictionary dictionary) {
        this.tableName = dictionary.getJsonAliasFor(tableClass);
        this.id = tableName + "." + fieldName;
        this.name = fieldName;
        this.columnTags = new HashSet<>();

        Meta meta = dictionary.getAttributeOrRelationAnnotation(tableClass, Meta.class, fieldName);
        if (meta != null) {
            this.longName = meta.longName();
            this.description = meta.description();
        }

        dataType = getDataType(tableClass, fieldName, dictionary);
        if (dataType == null) {
            throw new IllegalArgumentException("Unknown data type for " + this.id);
        }
    }

    public static DataType getDataType(Class<?> tableClass, String fieldName, EntityDictionary dictionary) {
        String tableName = dictionary.getJsonAliasFor(tableClass);
        DataType dataType;
        if (dictionary.isRelation(tableClass, fieldName)) {
            Class<?> relationshipClass = dictionary.getParameterizedType(tableClass, fieldName);
            dataType = new RelationshipType(dictionary.getJsonAliasFor(relationshipClass));
        } else {
            Class<?> fieldClass = dictionary.getType(tableClass, fieldName);

            if (fieldName.equals(dictionary.getIdFieldName(tableClass))) {
                dataType = new DataType(tableName + "." + fieldName, ValueType.ID);
            } else if (Date.class.isAssignableFrom(fieldClass)) {
                dataType = new DataType(fieldClass.getSimpleName().toLowerCase(Locale.ENGLISH), ValueType.DATE);
            } else {
                dataType = DataType.getScalarType(fieldClass);
            }
        }
        return dataType;
    }
}
