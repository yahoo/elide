/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata.models;

import com.yahoo.elide.annotation.Include;

import lombok.Data;
import lombok.ToString;

import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * Arguments that can be provided into a metric function
 */
@Include(rootLevel = true, type = "functionArgument")
@Entity
@Data
@ToString
public class FunctionArgument {
    @Id
    private String name;

    private String description;

    private DataType valueType;
}
