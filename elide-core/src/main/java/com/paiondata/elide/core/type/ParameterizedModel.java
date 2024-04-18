/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.core.type;

import com.paiondata.elide.annotation.Exclude;
import com.paiondata.elide.core.exceptions.InvalidParameterizedAttributeException;
import com.paiondata.elide.core.request.Argument;
import com.paiondata.elide.core.request.Attribute;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Base class that contains one or more parameterized attributes.
 */
public abstract class ParameterizedModel implements Serializable {
    private static final long serialVersionUID = -519263564697315522L;

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
        return parameterizedAttributes.entrySet().stream()

                //Only filter by alias required.  (Filtering by type may not work with inheritance).
                .filter(entry -> attribute.getAlias().equals(entry.getKey().getAlias()))
                .findFirst()
                .map(Map.Entry::getValue)
                .orElseThrow(() -> new InvalidParameterizedAttributeException(attribute))
                .invoke(attribute.getArguments());
    }

    /**
     * Fetch the attribute value by name.
     * @param alias The field name to fetch.
     * @param defaultValue Returned if the field name is not found
     * @param <T> The return type of the attribute.
     * @return The attribute value or the provided default value.
     */
    public <T> T fetch(String alias, T defaultValue) {
        Optional<ParameterizedAttribute> match = parameterizedAttributes.entrySet().stream()

                //Only filter by alias required.  (Filtering by type may not work with inheritance).
                .filter(entry -> alias.equals(entry.getKey().getAlias()))
                .findFirst()
                .map(Map.Entry::getValue);

        if (! match.isPresent()) {
            return defaultValue;
        }

        return match.get().invoke(new HashSet<>());
    }
}
