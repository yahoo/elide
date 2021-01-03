/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.dictionary;

import com.yahoo.elide.core.type.Type;

import lombok.Getter;

/**
 * Argument Type wraps an argument to the type of value it accepts.
 */
public class ArgumentType {
    @Getter
    private String name;
    @Getter
    private Type<?> type;

    public ArgumentType(String name, Type<?> type) {
        this.name = name;
        this.type = type;
    }
}
