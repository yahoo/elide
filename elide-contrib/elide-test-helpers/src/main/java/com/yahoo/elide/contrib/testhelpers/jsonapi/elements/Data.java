/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.contrib.testhelpers.jsonapi.elements;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.LinkedHashMap;

/**
 * The type Data.
 */
public class Data extends LinkedHashMap<String, Object> {
    static private final Gson GSON_INSTANCE = new GsonBuilder()
            .serializeNulls().create();

    /**
     * Instantiates a new Data based on resources.
     *
     * @param resources the resources
     */
    public Data(Resource... resources) {
        if (resources == null) {
            this.put("data", new Resource[0]);
        }

        // PATCH method does not work on an array of resources, hence sending it as a single element
        else if (resources.length == 1) {
            this.put("data", resources[0]);
        }
        else {
            this.put("data", resources);
        }
    }

    /**
     * Instantiates a new Data based on relationships.
     *
     * @param links the relationships
     */
    public Data(ResourceLinkage... links) {
        this.put("data", links);
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
