/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.cache;

import com.yahoo.elide.datastores.aggregation.query.QueryResult;

import com.github.benmanes.caffeine.cache.Caffeine;

public class CaffeineCache implements Cache {
    private final com.github.benmanes.caffeine.cache.Cache<Object,QueryResult> cache;

    public CaffeineCache(int maximumSize) {
        cache = Caffeine.newBuilder().maximumSize(maximumSize).recordStats().build();
    }

    @Override
    public QueryResult get(Object key) {
        return cache.getIfPresent(key);
    }

    @Override
    public void put(Object key, QueryResult result) {
        cache.put(key, result);
    }
}
