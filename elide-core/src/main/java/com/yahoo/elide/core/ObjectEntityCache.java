/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Cache to store object entity.
 */
public class ObjectEntityCache {
    private final Map<String, Object> resourceCache;
    private final Map<Object, String> uuidReverseMap;

    /**
     * Constructor.
     */
    public ObjectEntityCache() {
        resourceCache = new LinkedHashMap<>();
        uuidReverseMap = new IdentityHashMap<>();
    }

    /**
     * Add a resource to cache.
     *
     * @param type the type
     * @param id the id
     * @param entity the entity
     * @return the object
     */
    public Object put(String type, String id, Object entity) {
        uuidReverseMap.put(entity, id);
        return resourceCache.put(getCacheKey(type, id), entity);
    }

    /**
     * Retrieve object entity from cache.
     *
     * @param type the type
     * @param id the id
     * @return object
     */
    public Object get(String type, String id) {
        return resourceCache.get(getCacheKey(type, id));
    }

    /**
     * Get a UUID for an entity object.
     *
     * @param obj the obj
     * @return uUID
     */
    public String getUUID(Object obj) {
        return uuidReverseMap.get(obj);
    }

    /**
     * Get key for resource cache.
     *
     * @param type the type
     * @param id the id
     * @return key
     */
    private static String getCacheKey(String type, String id) {
        return type + "_" + id;
    }
}
