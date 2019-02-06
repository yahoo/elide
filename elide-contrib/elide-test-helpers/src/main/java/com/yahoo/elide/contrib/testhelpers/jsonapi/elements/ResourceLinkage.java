/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.contrib.testhelpers.jsonapi.elements;

import java.util.LinkedHashMap;

/**
 * The type Resource linkage.
 */
public class ResourceLinkage extends LinkedHashMap<String, Object> {
    /**
     * Instantiates a new Resource linkage.
     *
     * @param id   the id
     * @param type the type
     */
    public ResourceLinkage(Id id, Type type) {
       this.put("id", id.value);
       this.put("type", type.value);
    }
}
