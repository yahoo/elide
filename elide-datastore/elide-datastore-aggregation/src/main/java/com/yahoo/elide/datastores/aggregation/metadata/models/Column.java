/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata.models;

import com.yahoo.elide.datastores.aggregation.AggregationDictionary;
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
@Data
@ToString
public abstract class Column {
    @Id
    private String id;

    private String name;

    private String longName;

    private String description;

    private String category;

    @ManyToOne
    private DataType dataType;

    @ToString.Exclude
    private Set<Tag> columnTags;

    protected Column(Class<?> tableClass, String fieldName, AggregationDictionary dictionary) {
        this.id = dictionary.getJsonAliasFor(tableClass) + "." + fieldName;
        this.name = fieldName;
        this.columnTags = new HashSet<>();

        if (dictionary.isRelation(tableClass, fieldName)) {
            Class<?> relationshipClass = dictionary.getParameterizedType(tableClass, fieldName);
            String relationshipName = dictionary.getJsonAliasFor(relationshipClass);
            this.dataType = new RelationshipType(
                    this.id + "." + relationshipClass.getSimpleName().toLowerCase(Locale.ENGLISH), relationshipName);
        } else {
            Class<?> fieldClass = dictionary.getType(tableClass, fieldName);

            if (dictionary.getIdFieldName(tableClass).equals(fieldName)) {
                this.dataType = new DataType(dictionary.getJsonAliasFor(tableClass) + "." + fieldName, ValueType.ID);
            } else if (Date.class.isAssignableFrom(fieldClass)) {
                this.dataType = new DataType(fieldClass.getSimpleName().toLowerCase(Locale.ENGLISH), ValueType.DATE);
            } else {
                DataType dataType = DataType.getScalarType(fieldClass);
                if (dataType == null) {
                    throw new IllegalArgumentException("Unknown data type for " + this.id);
                }
                this.dataType = dataType;
            }
        }
    }
}
