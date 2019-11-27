/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata.models;

import com.yahoo.elide.annotation.Exclude;
import com.yahoo.elide.datastores.aggregation.metadata.enums.ValueType;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Special data type that represents a relationship between tables.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RelationshipType extends DataType {
    public RelationshipType(String name) {
        super(name, ValueType.RELATIONSHIP);
    }
}
