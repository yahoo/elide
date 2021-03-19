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
import com.yahoo.elide.datastores.aggregation.query.QueryPlanResolver;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Column which supports aggregation.
 */
@Include(rootLevel = false, type = "metric")
@Getter
@EqualsAndHashCode(callSuper = true)
@ToString
public class Metric extends Column {
    @Exclude
    @ToString.Exclude
    private final QueryPlanResolver queryPlanResolver;

    public Metric(Table table, String fieldName, EntityDictionary dictionary) {
        super(table, fieldName, dictionary);
        Type<?> tableClass = dictionary.getEntityClass(table.getName(), table.getVersion());

        MetricFormula formula = dictionary.getAttributeOrRelationAnnotation(
                tableClass,
                MetricFormula.class,
                fieldName);

        if (formula != null) {
            this.queryPlanResolver = dictionary.getInjector().instantiate(formula.queryPlan());
            dictionary.getInjector().inject(this.queryPlanResolver);

        } else {
            throw new IllegalStateException("Trying to construct metric field "
                    + getId() + " without @MetricFormula.");
        }
    }
}
