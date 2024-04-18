/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.test.jsonapi.elements;

import java.util.LinkedHashMap;

public class Links extends LinkedHashMap<String, Object> {
    /**
     * Instantiates a new Attributes.
     *
     * @param attributes the attributes
     */
    public Links(Attribute... attributes) {
        for (Attribute attribute : attributes) {
            this.put(attribute.key, attribute.value);
        }
    }
}
