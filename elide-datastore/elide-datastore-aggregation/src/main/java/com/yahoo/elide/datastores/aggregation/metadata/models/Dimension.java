/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata.models;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.datastores.aggregation.annotation.ColumnMeta;
import com.yahoo.elide.datastores.aggregation.annotation.DimensionFormula;

import lombok.EqualsAndHashCode;

import java.util.HashSet;

/**
 * Regular field in tables, can be grouped by.
 */
@Include(rootLevel = false, type = "dimension")
@EqualsAndHashCode(callSuper = true)
public class Dimension extends Column {
    public Dimension(Table table, String fieldName, EntityDictionary dictionary) {
        super(table, fieldName, dictionary, getConstructFunction(table, fieldName, dictionary));
    }

    protected static ConstructFunction getConstructFunction(Table table, String fieldName,
            EntityDictionary dictionary) {
        return () -> {
            Type<?> tableClass = dictionary.getEntityClass(table.getName(), table.getVersion());
            DimensionFormula formula = dictionary.getAttributeOrRelationAnnotation(
                    tableClass,
                    DimensionFormula.class,
                    fieldName);
            if (formula == null) {
                // Physical Reference, not using @DimensionFormula
                return null;
            } else {
                ColumnMeta meta = dictionary.getAttributeOrRelationAnnotation(
                        tableClass,
                        ColumnMeta.class,
                        fieldName);
                String id = constructColumnName(tableClass, fieldName, dictionary) + "[" + fieldName + "]";
                String description = meta == null ? null : meta.description();
                return new Function(id, description, new HashSet<>());
            }
        };
    }
}
