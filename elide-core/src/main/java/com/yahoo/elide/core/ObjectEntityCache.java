/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Cache to store object entity.
 */
public class ObjectEntityCache {
    private static final int MAX_CACHE_SIZE = 10000;
    private final Map<String, Object> resourceCache;
    private final Map<Object, String> uuidReverseMap;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * Constructor.
     */
    public ObjectEntityCache() {
        resourceCache = new LinkedHashMap<String, Object>(16, 0.75f, true);
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
        lock.writeLock().lock();
        try {
            uuidReverseMap.put(entity, id);

            if (resourceCache.size() >= MAX_CACHE_SIZE) {
                evictLRU();
            }

            return resourceCache.put(getCacheKey(type, id), entity);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void evictLRU() {
        if (resourceCache.isEmpty()) {
            return;
        }

        String oldestKey = resourceCache.keySet().iterator().next();
        Object evictedEntity = resourceCache.get(oldestKey);
        resourceCache.remove(oldestKey);
        uuidReverseMap.remove(evictedEntity);
    }

    private String getCacheKeyFromUUID(String uuidValue) {
        for (String key : resourceCache.keySet()) {
            if (key.endsWith("_" + uuidValue)) {
                return key;
            }
        }
        return null;
    }

    /**
     * Retrieve object entity from cache.
     *
     * @param type the type
     * @param id the id
     * @return object
     */
    public Object get(String type, String id) {
        lock.writeLock().lock();
        try {
            return resourceCache.get(getCacheKey(type, id));
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Get a UUID for an entity object.
     *
     * @param obj the obj
     * @return uUID
     */
    public String getUUID(Object obj) {
        lock.readLock().lock();
        try {
            return uuidReverseMap.get(obj);
        } finally {
            lock.readLock().unlock();
        }
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
