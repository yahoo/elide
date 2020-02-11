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

/**
 * Regular field in tables, can be grouped by.
 */
@EqualsAndHashCode(callSuper = true)
@Include(type = "dimension")
@Data
public class Dimension extends Column {
    public Dimension(Class<?> tableClass, String fieldName, EntityDictionary dictionary) {
        super(tableClass, fieldName, dictionary);
    }
}
