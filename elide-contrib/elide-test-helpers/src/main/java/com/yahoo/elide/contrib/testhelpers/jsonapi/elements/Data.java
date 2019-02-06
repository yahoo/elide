/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.contrib.testhelpers.jsonapi.elements;

import com.google.gson.Gson;

import java.util.LinkedHashMap;

/**
 * The type Data.
 */
public class Data extends LinkedHashMap<String, Object> {
    static private final Gson GSON_INSTANCE = new Gson();

    /**
     * Instantiates a new Data.
     *
     * @param resources the resources
     */
    public Data(Resource... resources) {
        // PATCH method does not work on an array of resources, hence sending it as a single element
        if (resources.length == 1) {
            this.put("data", resources[0]);
        }
        else {
            this.put("data", resources);
        }
    }

    /**
     * To json string.
     *
     * @return the string
     */
    public String toJSON() {
        return GSON_INSTANCE.toJson(this);
    }
}
