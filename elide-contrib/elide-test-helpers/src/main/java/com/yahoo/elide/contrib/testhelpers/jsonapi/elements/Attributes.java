/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.contrib.testhelpers.jsonapi.elements;

import java.util.LinkedHashMap;

/**
 * The type Attributes.
 */
public class Attributes extends LinkedHashMap {

    /**
     * Instantiates a new Attributes.
     *
     * @param attributes the attributes
     */
    public Attributes(Attribute... attributes) {
        for (Attribute attribute : attributes) {
            this.put(attribute.key, attribute.value);
        }
    }
}
