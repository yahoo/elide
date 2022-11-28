/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.core.request;

import com.yahoo.elide.core.type.ClassType;
import com.yahoo.elide.core.type.Type;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.Singular;
import lombok.ToString;

import java.io.Serializable;
import java.util.Set;

/**
 * Represents an attribute on an Elide entity.  Attributes can take arguments.
 */
@Data
@Builder
public class Attribute implements Serializable {
    private static final long serialVersionUID = 3009706331255770579L;

    @NonNull
    @ToString.Exclude
    private Type<?> type;

    @NonNull
    private String name;

    @ToString.Exclude
    private String alias;

    @Singular
    @ToString.Exclude
    private Set<Argument> arguments;

    private Attribute(@NonNull Type<?> type, @NonNull String name, String alias, Set<Argument> arguments) {
        this.type = type;
        this.name = name;
        this.alias = alias == null ? name : alias;
        this.arguments = arguments;
    }

    public static class AttributeBuilder {

        public Attribute.AttributeBuilder type(Type<?> type) {
            this.type = type;
            return this;
        }

        public Attribute.AttributeBuilder type(Class<?> type) {
            this.type = ClassType.of(type);
            return this;
        }
    }
}
