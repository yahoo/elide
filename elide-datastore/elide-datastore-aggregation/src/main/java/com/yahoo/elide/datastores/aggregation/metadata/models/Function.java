/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata.models;

import com.yahoo.elide.annotation.Include;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

import java.util.Set;
import javax.persistence.Id;
import javax.persistence.OneToMany;

/**
 * Functions used in tables, dimension, metrics.
 */
@Include(rootLevel = false, type = "function")
@Data
@ToString
@AllArgsConstructor
public class Function {
    @Id
    private String name;

    private String description;

    @OneToMany
    private Set<FunctionArgument> arguments;
}
