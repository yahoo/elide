/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.request;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.Singular;
import lombok.ToString;

import java.util.Set;

/**
 * Represents an attribute on an Elide entity.  Attributes can take arguments.
 */
@Data
@Builder
public class Attribute {
    @NonNull
    @ToString.Exclude
    private Class<?> type;

    @NonNull
    private String name;

    @ToString.Exclude
    private String alias;

    @ToString.Exclude
    //If null, the parentType is the same as the entity projection to which this attribute belongs.
    //If not null, this represents the model type where this attribute can be found.
    private Class<?> parentType;

    @Singular
    @ToString.Exclude
    private Set<Argument> arguments;

    private Attribute(@NonNull Class<?> type, @NonNull String name, String alias, Class<?> parentType,
                      Set<Argument> arguments) {
        this.type = type;
        this.parentType = parentType;
        this.name = name;
        this.alias = alias == null ? name : alias;
        this.arguments = arguments;
    }

    private Attribute(@NonNull Class<?> type, @NonNull String name, String alias, Set<Argument> arguments) {
        this.type = type;
        this.parentType = null;
        this.name = name;
        this.alias = alias == null ? name : alias;
        this.arguments = arguments;
    }
}
