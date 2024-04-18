/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.core.type;

import com.paiondata.elide.core.request.Argument;

import java.io.Serializable;
import java.util.Set;

/**
 * An elide attribute that supports parameters.
 */
@FunctionalInterface
public interface ParameterizedAttribute extends Serializable {

    /**
     * Fetch the attribute value with the specified parameters.
     * @param arguments The attribute arguments.
     * @param <T> The return type of the attribute.
     * @return The attribute value.
     */
    public <T> T invoke(Set<Argument> arguments);
}
