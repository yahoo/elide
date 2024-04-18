/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.test.jsonapi.elements;

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
       this.put("type", type.value);
       this.put("id", id.value);
    }

    /**
     * Instantiates a new Resource linkage.
     *
     * @param lid   the lid
     * @param type the type
     */
    public ResourceLinkage(Lid lid, Type type) {
       this.put("type", type.value);
       this.put("lid", lid.value);
    }
}
