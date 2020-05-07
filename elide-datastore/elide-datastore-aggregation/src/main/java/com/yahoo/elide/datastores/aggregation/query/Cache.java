/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.query;

/**
 * A cache for Query results.
 */
public interface Cache {
    /**
     * Load Query result from cache. Exceptions should be passed through.
     *
     * @param key    a key to look up in the cache.
     * @return query results from cache, or null if not found.
     */
    Iterable<Object> get(Object key);

    /**
     * Insert results into cache.
     *
     * @param key    the key to associate with the query
     * @param result the result to cache with the key
     */
    void put(Object key, Iterable<Object> result);
}
