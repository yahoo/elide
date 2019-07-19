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

import java.util.Set;

/**
 * Represents an attribute on an Elide entity.  Attributes can take arguments.
 */
@Data
@Builder
public class Attribute {
    @NonNull
    private DataCollection parent;

    @NonNull
    private String name;

    @Singular
    private Set<Argument> arguments;

    /**
     * Returns the attribute type.
     * @return the attribute type.
     */
    public Class<?> getType() {
        return parent.getDictionary().getType(parent.getType(), name);
    }
}
