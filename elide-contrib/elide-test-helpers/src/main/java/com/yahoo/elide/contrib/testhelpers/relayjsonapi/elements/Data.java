/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.testhelpers.relayjsonapi.elements;

import com.yahoo.elide.contrib.testhelpers.jsonapi.elements.Attribute;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Collections;
import java.util.LinkedHashMap;

/**
 *
 */
public class Data extends LinkedHashMap<String, Object> {

    static private final Gson GSON_INSTANCE = new GsonBuilder()
            .serializeNulls().create();

    public Data(Resource resource) {
        Attribute attribute = (Attribute) resource;
        this.put("data", Collections.singletonMap(attribute.getKey(), attribute.getValue()));
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
