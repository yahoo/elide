/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

import lombok.Getter;

/**
 * Argument Type wraps an argument to the type of value it accepts.
 */
public class ArgumentType {
    @Getter
    private String name;
    @Getter
    private Class<?> type;

    public ArgumentType(String name, Class<?> type) {
        this.name = name;
        this.type = type;
    }
}
