/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.contrib.testhelpers.jsonapi.elements;

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
        // PATCH method does not work on an array of resources, hence sending it as a single element
        if (resources.length == 1) {
            this.put("included", resources[0]);
        } else {
            this.put("included", resources);
        }
    }
}
