/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata.models;

import static com.yahoo.elide.datastores.aggregation.metadata.enums.ColumnType.FIELD;
import static com.yahoo.elide.datastores.aggregation.metadata.enums.ColumnType.FORMULA;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.ToOne;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.type.ClassType;
import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.datastores.aggregation.annotation.CardinalitySize;
import com.yahoo.elide.datastores.aggregation.annotation.ColumnMeta;
import com.yahoo.elide.datastores.aggregation.annotation.DimensionFormula;
import com.yahoo.elide.datastores.aggregation.annotation.MetricFormula;
import com.yahoo.elide.datastores.aggregation.metadata.enums.ColumnType;
import com.yahoo.elide.datastores.aggregation.metadata.enums.ValueSourceType;
import com.yahoo.elide.datastores.aggregation.metadata.enums.ValueType;
import com.yahoo.elide.datastores.aggregation.query.ColumnProjection;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.Id;
import javax.persistence.OneToMany;

/**
 * Column is the super class of a field in a table, it can be either dimension or metric.
 */
@Include(rootLevel = false, type = "column")
@Getter
@EqualsAndHashCode
@ToString
public abstract class Column implements Versioned {

    @Id
    private final String id;

    private final String name;

    private final String friendlyName;

    private final String category;

    private final String description;

    private final CardinalitySize cardinality;

    @ToOne
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private final Table table;

    private final ValueType valueType;

    private final ColumnType columnType;

    private final String expression;

    private final ValueSourceType valueSourceType;

    private final Set<String> values;

    private final String tableSource;

    @OneToMany
    @ToString.Exclude
    private final Set<Argument> arguments;

    @ToString.Exclude
    private final Set<String> tags;

    protected Column(Table table, String fieldName, EntityDictionary dictionary) {
        this.table = table;
        Type<?> tableClass = dictionary.getEntityClass(table.getName(), table.getVersion());

        this.id = constructColumnName(tableClass, fieldName, dictionary);
        this.name = fieldName;

        ColumnMeta meta = dictionary.getAttributeOrRelationAnnotation(tableClass, ColumnMeta.class, fieldName);
        if (meta != null) {
            this.friendlyName = meta.friendlyName() != null && !meta.friendlyName().isEmpty()
                    ? meta.friendlyName()
                    : name;
            this.description = meta.description();
            this.category = meta.category();
            this.values = new HashSet<>(Arrays.asList(meta.values()));
            this.tags = new HashSet<>(Arrays.asList(meta.tags()));
            this.tableSource = (meta.tableSource().trim().isEmpty()) ? null : meta.tableSource();
            this.valueSourceType = ValueSourceType.getValueSourceType(this.values,
                    this.tableSource);
            this.cardinality = meta.size();
        } else {
            this.friendlyName = name;
            this.description = null;
            this.category = null;
            this.values = null;
            this.tags = new HashSet<>();
            this.tableSource = null;
            this.valueSourceType = ValueSourceType.NONE;
            this.cardinality = CardinalitySize.UNKNOWN;
        }

        if (dictionary.attributeOrRelationAnnotationExists(tableClass, fieldName, MetricFormula.class)) {
            columnType = FORMULA;
            expression = dictionary
                    .getAttributeOrRelationAnnotation(tableClass, MetricFormula.class, fieldName).value();
            MetricFormula metricFormula = dictionary.getAttributeOrRelationAnnotation(tableClass, MetricFormula.class,
                    fieldName);
            this.arguments = Arrays.stream(metricFormula.arguments())
                    .map(argument -> new Argument(getId(), argument))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        } else if (dictionary.attributeOrRelationAnnotationExists(tableClass, fieldName, DimensionFormula.class)) {
            columnType = FORMULA;
            expression = dictionary
                    .getAttributeOrRelationAnnotation(tableClass, DimensionFormula.class, fieldName).value();
            DimensionFormula dimensionFormula = dictionary.getAttributeOrRelationAnnotation(tableClass,
                    DimensionFormula.class, fieldName);
            this.arguments = Arrays.stream(dimensionFormula.arguments())
                    .map(argument -> new Argument(getId(), argument))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        } else {
            columnType = FIELD;
            expression = dictionary.getAnnotatedColumnName(tableClass, fieldName);
            this.arguments = new HashSet<>();
        }

        this.valueType = getValueType(tableClass, fieldName, dictionary);

        if (valueType == null) {
            throw new IllegalArgumentException("Unknown data type for " + this.id);
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
    protected static String constructColumnName(Type<?> tableClass, String fieldName, EntityDictionary dictionary) {
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
    public static ValueType getValueType(Type<?> tableClass, String fieldName, EntityDictionary dictionary) {
        if (dictionary.isRelation(tableClass, fieldName)) {
            return ValueType.RELATIONSHIP;
        }
        Type<?> fieldClass = dictionary.getType(tableClass, fieldName);

        if (fieldName.equals(dictionary.getIdFieldName(tableClass))) {
            return ValueType.ID;
        }
        if (ClassType.DATE_TYPE.isAssignableFrom(fieldClass)) {
            return ValueType.TIME;
        }
        return ValueType.getScalarType(fieldClass);
    }

    public ColumnProjection toProjection() {
        return table.toQueryable().getColumnProjection(getName());
    }

    @Override
    public String getVersion() {
        return table.getVersion();
    }
}
