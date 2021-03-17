/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata.models;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.datastores.aggregation.metadata.enums.ValueSourceType;
import com.yahoo.elide.datastores.aggregation.metadata.enums.ValueType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

import java.util.Set;

import javax.persistence.Id;

/**
 * Arguments that can be provided into a function.
 */
@Include(rootLevel = false, type = "functionArgument")
@Data
@ToString
@AllArgsConstructor
public class FunctionArgument {
    @Id
    private String id;

    private String name;

    private String description;

    private ValueType type;

    private final ValueSourceType valueSourceType;

    private Set<String> values;

    private String tableSource;

    private Object defaultValue;

    public FunctionArgument(String functionName, FunctionArgument argument) {
        this.id = functionName + "." + argument.getName();
        this.name = argument.getName();
        this.description = argument.getDescription();
        this.type = argument.getType();
        this.values = argument.getValues();
        this.tableSource = argument.getTableSource();
        this.defaultValue = argument.getDefaultValue();
        this.valueSourceType = ValueSourceType.getValueSourceType(this.values, this.tableSource);
    }
}
