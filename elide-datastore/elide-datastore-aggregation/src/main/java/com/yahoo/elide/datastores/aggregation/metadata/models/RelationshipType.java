/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata.models;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.datastores.aggregation.metadata.enums.ValueType;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.Entity;

/**
 * Special data type that represents a relationship between tables.
 */
@Entity
@Include(rootLevel = true, type = "relationshipType")
@Data
@EqualsAndHashCode(callSuper = true)
public class RelationshipType extends DataType {
    private String tableName;

    public RelationshipType(String name, String tableName) {
        super(name, ValueType.RELATIONSHIP);
        this.tableName = tableName;
    }
}
