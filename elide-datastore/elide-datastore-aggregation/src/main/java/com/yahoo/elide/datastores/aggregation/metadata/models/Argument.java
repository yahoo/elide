/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata.models;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.datastores.aggregation.annotation.ArgumentDefinition;
import com.yahoo.elide.datastores.aggregation.metadata.enums.ValueSourceType;
import com.yahoo.elide.datastores.aggregation.metadata.enums.ValueType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.Id;

/**
 * Arguments that can be provided into a table/column.
 */
@Include(rootLevel = false, type = "argument")
@Data
@ToString
@AllArgsConstructor
public class Argument {
    @Id
    private String id;

    private String name;

    private String description;

    private ValueType type;

    private final ValueSourceType valueSourceType;

    private Set<String> values;

    private String tableSource;

    private Object defaultValue;

    public Argument(String idPrefix, ArgumentDefinition argument) {
        this.id = idPrefix + "." + argument.name();
        this.name = argument.name();
        this.description = argument.description();
        this.type = argument.type();
        this.values = new HashSet<>(Arrays.asList(argument.values()));
        this.tableSource = argument.tableSource();
        this.defaultValue = argument.defaultValue();
        this.valueSourceType = ValueSourceType.getValueSourceType(this.values, this.tableSource);
    }
}
