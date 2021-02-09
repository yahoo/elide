/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.core.type;

import com.yahoo.elide.annotation.Exclude;
import com.yahoo.elide.core.exceptions.InvalidParameterizedAttributeException;
import com.yahoo.elide.core.request.Argument;
import com.yahoo.elide.core.request.Attribute;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Base class that contains one or more parameterized attributes.
 */
public abstract class ParameterizedModel {

    @Exclude
    protected Map<Attribute, ParameterizedAttribute> parameterizedAttributes;

    public ParameterizedModel() {
        this(new HashMap<>());
    }

    public ParameterizedModel(Map<Attribute, ParameterizedAttribute> attributes) {
        this.parameterizedAttributes = attributes;
    }

    public <T> void addAttributeValue(Attribute attribute, T value) {
        parameterizedAttributes.put(attribute,
            new ParameterizedAttribute() {
                @Override
                public <T> T invoke(Set<Argument> arguments) {
                    return (T) value;
                }
            });
    }

    /**
     * Fetch the attribute value with the specified parameters.
     * @param attribute The attribute to fetch.
     * @param <T> The return type of the attribute.
     * @return The attribute value.
     */
    public <T> T invoke(Attribute attribute) {
        Optional<Attribute> match = parameterizedAttributes.keySet().stream()

                //Only filter by alias required.  (Filtering by type may not work with inheritance).
                .filter((modelAttribute) -> attribute.getAlias().equals(modelAttribute.getAlias()))
                .findFirst();

        if (! match.isPresent()) {
            throw new InvalidParameterizedAttributeException(attribute);
        }

        return parameterizedAttributes.get(match.get()).invoke(attribute.getArguments());
    }

    /**
     * Fetch the attribute value by name.
     * @param alias The field name to fetch.
     * @param defaultValue Returned if the field name is not found
     * @param <T> The return type of the attribute.
     * @return The attribute value or the provided default value.
     */
    public <T> T fetch(String alias, T defaultValue) {
        Optional<Attribute> match = parameterizedAttributes.keySet().stream()

                //Only filter by alias required.  (Filtering by type may not work with inheritance).
                .filter((modelAttribute) -> alias.equals(modelAttribute.getAlias()))
                .findFirst();

        if (! match.isPresent()) {
            return defaultValue;
        }

        return parameterizedAttributes.get(match.get()).invoke(new HashSet<>());
    }
}
