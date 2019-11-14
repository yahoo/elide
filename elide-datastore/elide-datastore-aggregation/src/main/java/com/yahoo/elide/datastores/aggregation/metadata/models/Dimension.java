/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata.models;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.datastores.aggregation.AggregationDictionary;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.Entity;

/**
 * Regular field in tables, can be grouped by if the table is an AnalyticView
 */
@EqualsAndHashCode(callSuper = true)
@Include(type = "dimension")
@Entity
@Data
public class Dimension extends Column {
    public Dimension(Class<?> tableClass, String fieldName, AggregationDictionary dictionary) {
        super(tableClass, fieldName, dictionary);
    }
}
