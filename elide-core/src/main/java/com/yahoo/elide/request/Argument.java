/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.request;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

/**
 * Represents an argument passed to an attribute.
 */
@Data
@Builder
public class Argument {

    @NonNull
    String name;

    Object value;

    /**
     * Returns the argument type.
     * @return the argument type.
     */
    public Class<?> getType() {
        return value.getClass();
    }
}
