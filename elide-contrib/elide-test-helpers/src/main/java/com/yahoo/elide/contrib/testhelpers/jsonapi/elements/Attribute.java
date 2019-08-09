/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.testhelpers.jsonapi.elements;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * The type Attribute.
 */
@AllArgsConstructor
public class Attribute {
    /**
     * The Key.
     */
    @Getter
    final String key;
    /**
     * The Value.
     */
    @Getter
    final Object value;
}
