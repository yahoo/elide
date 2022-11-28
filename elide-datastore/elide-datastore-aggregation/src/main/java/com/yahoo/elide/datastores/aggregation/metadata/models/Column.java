/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata.models;

import static com.yahoo.elide.datastores.aggregation.metadata.enums.ColumnType.FIELD;
import static com.yahoo.elide.datastores.aggregation.metadata.enums.ColumnType.FORMULA;

import com.yahoo.elide.annotation.Exclude;
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
import com.yahoo.elide.modelconfig.model.Named;

import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Column is the super class of a field in a table, it can be either dimension or metric.
 */
@Include(rootLevel = false, name = "column")
@Getter
@EqualsAndHashCode
@ToString
public abstract class Column implements Versioned, Named, RequiresFilter {

    @Id
    private final String id;

    private final String name;

    private final String friendlyName;

    private final String category;

    private final String description;

    private final CardinalitySize cardinality;

    private final boolean hidden;

    @ToOne
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private final Table table;

    private final ValueType valueType;

    private final ColumnType columnType;

    private final String expression;

    private final ValueSourceType valueSourceType;

    private final LinkedHashSet<String> values;

    private String requiredFilter;

    @OneToOne
    @Setter
    private TableSource tableSource = null;

    @Exclude
    private com.yahoo.elide.datastores.aggregation.annotation.TableSource tableSourceDefinition;

    @OneToMany
    @ToString.Exclude
    @Getter(value = AccessLevel.NONE)
    private final Set<ArgumentDefinition> arguments;

    public Set<ArgumentDefinition> getArgumentDefinitions() {
        return this.arguments;
    }

    @ToString.Exclude
    private final Set<String> tags;

    protected Column(Table table, String fieldName, EntityDictionary dictionary) {
        this.table = table;
        Type<?> tableClass = dictionary.getEntityClass(table.getName(), table.getVersion());

        this.id = constructColumnName(tableClass, fieldName, dictionary);
        this.name = fieldName;

        String idField = dictionary.getIdFieldName(tableClass);
        if (idField != null && idField.equals(fieldName)) {
            this.hidden = false;
        } else {
            this.hidden = !dictionary.getAllExposedFields(tableClass).contains(fieldName);
        }

        ColumnMeta meta = dictionary.getAttributeOrRelationAnnotation(tableClass, ColumnMeta.class, fieldName);
        if (meta != null) {
            this.friendlyName = meta.friendlyName() != null && !meta.friendlyName().isEmpty()
                    ? meta.friendlyName()
                    : name;
            this.description = meta.description();
            this.category = meta.category();
            this.values = new LinkedHashSet<>(Arrays.asList(meta.values()));
            this.tags = new LinkedHashSet<>(Arrays.asList(meta.tags()));
            this.tableSourceDefinition = meta.tableSource();
            this.valueSourceType = ValueSourceType.getValueSourceType(this.values, this.tableSourceDefinition);
            this.cardinality = meta.size();
            this.requiredFilter = meta.filterTemplate();
        } else {
            this.friendlyName = name;
            this.description = null;
            this.category = null;
            this.values = null;
            this.tags = new LinkedHashSet<>();
            this.tableSourceDefinition = null;
            this.valueSourceType = ValueSourceType.NONE;
            this.cardinality = CardinalitySize.UNKNOWN;
            this.requiredFilter = null;
        }

        if (dictionary.attributeOrRelationAnnotationExists(tableClass, fieldName, MetricFormula.class)) {
            columnType = FORMULA;
            MetricFormula metricFormula = dictionary.getAttributeOrRelationAnnotation(tableClass, MetricFormula.class,
                    fieldName);
            this.expression = metricFormula.value();
            this.arguments = Arrays.stream(metricFormula.arguments())
                    .map(argument -> new ArgumentDefinition(getId(), argument))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        } else if (dictionary.attributeOrRelationAnnotationExists(tableClass, fieldName, DimensionFormula.class)) {
            columnType = FORMULA;
            DimensionFormula dimensionFormula = dictionary.getAttributeOrRelationAnnotation(tableClass,
                    DimensionFormula.class, fieldName);
            this.expression = dimensionFormula.value();
            this.arguments = Arrays.stream(dimensionFormula.arguments())
                    .map(argument -> new ArgumentDefinition(getId(), argument))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        } else {
            columnType = FIELD;
            expression = "{{$" + dictionary.getAnnotatedColumnName(tableClass, fieldName) + "}}";
            this.arguments = new LinkedHashSet<>();
        }

        this.valueType = getValueType(tableClass, fieldName, dictionary);

        if (valueType == ValueType.UNKNOWN) {
            throw new IllegalArgumentException("Unknown data type for " + this.id);
        }
    }

    /**
     * Construct a column name as meta data.
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
     * Resolve the value type of a field.
     *
     * @param tableClass table class
     * @param fieldName field name
     * @param dictionary meta data dictionary
     * @return field value type
     */
    public static ValueType getValueType(Type<?> tableClass, String fieldName, EntityDictionary dictionary) {
        if (dictionary.isRelation(tableClass, fieldName)) {
            return ValueType.UNKNOWN;
        }
        Type<?> fieldClass = dictionary.getType(tableClass, fieldName);

        if (fieldName.equals(dictionary.getIdFieldName(tableClass))) {
            return ValueType.ID;
        }

        if (ClassType.DATE_TYPE.isAssignableFrom(fieldClass)) {
            return ValueType.TIME;
        }

        if (fieldClass.isEnum()) {
            return ValueType.TEXT;
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

    public boolean hasArgumentDefinition(String argName) {
        return hasName(this.arguments, argName);
    }

    public ArgumentDefinition getArgumentDefinition(String argName) {
        return this.arguments.stream()
                        .filter(arg -> arg.getName().equals(argName))
                        .findFirst()
                        .orElse(null);
    }
}
