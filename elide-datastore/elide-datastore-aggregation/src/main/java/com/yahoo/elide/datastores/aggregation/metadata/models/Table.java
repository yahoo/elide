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
}
