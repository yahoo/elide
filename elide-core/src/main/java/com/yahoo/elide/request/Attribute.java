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

    @Singular
    @ToString.Exclude
    private Set<Argument> arguments;
}
