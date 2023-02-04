/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata.models;

import com.yahoo.elide.annotation.Exclude;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.datastores.aggregation.annotation.MetricFormula;
import com.yahoo.elide.datastores.aggregation.query.MetricProjectionMaker;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Column which supports aggregation.
 */
@Include(rootLevel = false, name = "metric")
@Getter
@EqualsAndHashCode(callSuper = true)
@ToString
public class Metric extends Column {
    @Exclude
    @ToString.Exclude
    private final MetricProjectionMaker metricProjectionMaker;

    public Metric(MetricProjectionMaker maker, Table table, String fieldName, EntityDictionary dictionary) {
        super(table, fieldName, dictionary);
        this.metricProjectionMaker = maker;
    }

    public Metric(Table table, String fieldName, EntityDictionary dictionary) {
        super(table, fieldName, dictionary);
        Type<?> tableClass = dictionary.getEntityClass(table.getName(), table.getVersion());

        MetricFormula formula = dictionary.getAttributeOrRelationAnnotation(tableClass, MetricFormula.class, fieldName);

        verifyFormula(formula);
        this.metricProjectionMaker = dictionary.getInjector().instantiate(formula.maker());

        dictionary.getInjector().inject(this.metricProjectionMaker);
    }

    private void verifyFormula(MetricFormula formula) {
        if (formula == null) {
            throw new IllegalStateException("Trying to construct metric field " + getId() + " without @MetricFormula.");
        }

        String defaultValue;
        Class<?> defaultMaker;

        try {
            defaultValue = (String) MetricFormula.class.getDeclaredMethod("value").getDefaultValue();
            defaultMaker = (Class<?>) MetricFormula.class.getDeclaredMethod("maker").getDefaultValue();
        } catch (NoSuchMethodException | SecurityException e) {
            throw new IllegalStateException("Error encountered while constructing metric field: " + getId()
                            + ". " + e.getMessage());
        }

        if (formula.value().equals(defaultValue) && formula.maker().equals(defaultMaker)) {
            throw new IllegalStateException("Trying to construct metric field " + getId()
                            + " with default values. Provide either value or maker in @MetricFormula.");
        }

        if (!formula.value().equals(defaultValue) && !formula.maker().equals(defaultMaker)) {
            throw new IllegalStateException("Trying to construct metric field " + getId()
                            + " with value and maker. Provide either one in @MetricFormula, both are not allowed.");
        }
    }
}
