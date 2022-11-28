/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata.models;

import com.yahoo.elide.annotation.Exclude;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.datastores.aggregation.metadata.enums.ValueSourceType;
import com.yahoo.elide.datastores.aggregation.metadata.enums.ValueType;
import com.yahoo.elide.modelconfig.model.Named;

import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Arguments that can be provided into a table/column.
 */
@Include(rootLevel = false, name = "argument")
@Data
@ToString
@AllArgsConstructor
public class ArgumentDefinition implements Named {
    @Id
    private String id;

    private String name;

    private String description;

    private ValueType type;

    private final ValueSourceType valueSourceType;

    private Set<String> values;

    @OneToOne
    private TableSource tableSource;

    @Exclude
    private com.yahoo.elide.datastores.aggregation.annotation.TableSource tableSourceDefinition;

    private Object defaultValue;

    public boolean isRequired() {
        return (defaultValue == null || defaultValue.toString().equals(""));
    }

    public ArgumentDefinition(String idPrefix,
                              com.yahoo.elide.datastores.aggregation.annotation.ArgumentDefinition argument) {
        this.id = idPrefix + "." + argument.name();
        this.name = argument.name();
        this.description = argument.description();
        this.type = argument.type();
        this.values = new HashSet<>(Arrays.asList(argument.values()));
        this.tableSourceDefinition = argument.tableSource();
        this.defaultValue = argument.defaultValue();
        this.valueSourceType = ValueSourceType.getValueSourceType(this.values, this.tableSourceDefinition);
    }
}
