/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.core.request;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

/**
 * Represents an argument passed to an attribute.
 */
@Value
@Builder
public class Argument {

    @NonNull
    private final String name;

    private final Object value;

    /**
     * Returns the argument type.
     * @return the argument type.
     */
    public Class<?> getType() {
        return value.getClass();
    }
}
