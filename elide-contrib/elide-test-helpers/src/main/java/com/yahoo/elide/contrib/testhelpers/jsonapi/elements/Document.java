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
 * Top Level JSON-API Document.
 */
public class Document extends LinkedHashMap<String, Object> {
    static private final Gson GSON_INSTANCE = new GsonBuilder()
            .serializeNulls().create();

    /**
     * Instantiates a new Data based on resources.
     *
     * @param resources the resources
     */
    public Document(Resource... resources) {
        // PATCH method does not work on an array of resources, hence sending it as a single element
        if (resources.length == 1) {
            this.put("data", resources[0]);
        }
        else {
            this.put("data", resources);
        }
    }

    /**
     * Instantiates a document with resources and corresponding includes.
     * @param includes The includes
     * @param resources The resources
     */
    public Document(Data resources, Include includes) {
        this.put("data", resources.get("data"));
        this.put("included", includes.get("included"));
    }

    /**
     * Instantiates a document with relationships and corresponding includes.
     * @param includes The includes
     * @param links The relationships
     */
    public Document(Include includes, ResourceLinkage... links) {
        this.put("data", links);
        this.put("included", includes.get("included"));
    }

    /**
     * Instantiates a new Data based on relationship links.
     * @param links the relationships.
     */
    public Document(ResourceLinkage... links) {
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
