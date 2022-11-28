/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.dictionary;

import com.yahoo.elide.core.type.Type;

import lombok.Builder;
import lombok.Value;

/**
 * Argument Type wraps an argument to the type of value it accepts.
 */
@Value
public class ArgumentType {
    private String name;
    private Type<?> type;
    private Object defaultValue;

    public ArgumentType(String name, Type<?> type) {
        this(name, type, null);
    }

    @Builder
    public ArgumentType(String name, Type<?> type, Object defaultValue) {
        this.name = name;
        this.type = type;
        this.defaultValue = defaultValue;
    }
}
