/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata.models;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.core.EntityDictionary;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Set;
import java.util.stream.Collectors;
import javax.persistence.Entity;
import javax.persistence.OneToMany;

/**
 * AnalyticViews are logical tables that support aggregation functionality, but don't support join or relationship.
 */
@EqualsAndHashCode(callSuper = true)
@Include(rootLevel = true, type = "analyticView")
@Entity
@Data
public class AnalyticView extends Table {

    @OneToMany
    @ToString.Exclude
    private Set<Metric> metrics;

    @OneToMany
    @ToString.Exclude
    private Set<Dimension> dimensions;

    public AnalyticView(Class<?> cls, EntityDictionary dictionary) {
        super(cls, dictionary);

        metrics = getColumns().stream()
                .filter(col -> col instanceof Metric)
                .map(Metric.class::cast)
                .collect(Collectors.toSet());

        dimensions = getColumns().stream()
                .filter(col -> !(col instanceof Metric))
                .map(Dimension.class::cast)
                .collect(Collectors.toSet());
    }
}
