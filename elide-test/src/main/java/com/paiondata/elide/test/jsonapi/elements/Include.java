/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.test.jsonapi.elements;

import java.util.LinkedHashMap;

/**
 * The type Include.
 */
public class Include extends LinkedHashMap<String, Object> {
    /**
     * Instantiates a new Include based on resources.
     *
     * @param resources the resources
     */
    public Include(Resource... resources) {
        this.put("included", resources);
    }
}
