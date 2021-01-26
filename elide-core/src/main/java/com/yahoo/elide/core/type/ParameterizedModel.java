/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.core.type;

import com.yahoo.elide.core.exceptions.InvalidParameterizedAttributeException;
import com.yahoo.elide.core.request.Attribute;

import java.util.HashMap;
import java.util.Map;

/**
 * Base class that contains one or more parameterized attributes.
 */
public abstract class ParameterizedModel implements ParameterizedAttribute {
    private Map<Attribute, ParameterizedAttribute> parameterizedAttributes;

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
                public <T> T invoke(Attribute attribute) {
                    return (T) value;
                }
            });
    }

    @Override
    public <T> T invoke(Attribute attribute) {
        if (parameterizedAttributes.containsKey(attribute)) {
            throw new InvalidParameterizedAttributeException(attribute);
        }
        return parameterizedAttributes.get(attribute).invoke(attribute);
    }
}
