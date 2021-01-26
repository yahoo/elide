/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.core.type;

import com.yahoo.elide.core.request.Attribute;

/**
 * An elide model that supports parameterized attributes.
 */
public interface ParameterizedModel {

    /**
     * Fetch the attribute value with the specified parameters.
     * @param attribute The attribute to fetch.
     * @param <T> The return type of the attribute.
     * @return The attribute value.
     */
    public <T> T invoke(Attribute attribute);
}
