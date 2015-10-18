/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.jsonapi.models;

import java.util.Map;

/**
 * Map storing key-value and useful accessor functions.
 */
public abstract class KeyValMap {
    private final Map<String, Object> map;

    /**
     * Constructor.
     *
     * @param map Map to query
     */
    public KeyValMap(Map<String, Object> map) {
        this.map = map;
    }

    /**
     * Get an object from map.
     *
     * @param key Key index
     * @return Object mapped to key. null if does not exist
     */
    public Object getValue(String key) {
        return map.get(key);
    }

    /**
     * Get a typed object from map.
     *
     * @param key Key index
     * @param type the type
     * @param <T> genericClass
     * @return Object mapped to key. null if does not exist
     */
    public <T> T getValue(String key, Class<T> type) {
        return type.cast(map.get(key));
    }
}
