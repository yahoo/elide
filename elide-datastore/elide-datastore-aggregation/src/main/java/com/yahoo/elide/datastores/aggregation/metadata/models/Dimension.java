/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata.models;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.core.dictionary.EntityDictionary;

import lombok.EqualsAndHashCode;

/**
 * Regular field in tables, can be grouped by.
 */
@Include(rootLevel = false, name = "dimension")
@EqualsAndHashCode(callSuper = true)
public class Dimension extends Column {
    public Dimension(Table table, String fieldName, EntityDictionary dictionary) {
        super(table, fieldName, dictionary);
    }
}
