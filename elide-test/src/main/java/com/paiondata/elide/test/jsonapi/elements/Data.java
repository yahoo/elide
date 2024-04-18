/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.test.jsonapi.elements;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.LinkedHashMap;

/**
 * The type Data.
 */
public class Data extends LinkedHashMap<String, Object> {
    static private final Gson GSON_INSTANCE = new GsonBuilder()
            .disableHtmlEscaping()
            .serializeNulls().create();

    /**
     * Instantiates a singular data object from a single resource.
     * @param resource  the resources
     */
    public Data(Resource resource) {
        this.put("data", resource);
    }

    /**
     * Instantiates a data list based on multiple resources.
     *
     * @param resources the resources
     */
    public Data(Resource... resources) {
        if (resources == null) {
            this.put("data", new Resource[0]);
        } else {
            this.put("data", resources);
        }
    }

    /**
     * Instantiates a new data list based on relationships.
     *
     * @param links the relationships
     */
    public Data(ResourceLinkage... links) {
        this.put("data", links);
    }

    /**
     * Instantiates a singular data object based on a relationship.
     *
     * @param link the relationships
     */
    public Data(ResourceLinkage link) {
        this.put("data", link);
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
