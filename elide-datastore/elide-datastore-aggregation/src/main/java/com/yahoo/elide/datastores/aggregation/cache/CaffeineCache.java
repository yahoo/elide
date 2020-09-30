/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.cache;

import com.yahoo.elide.datastores.aggregation.query.QueryResult;

import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.concurrent.TimeUnit;

/**
 * A basic local-only cache.
 */
public class CaffeineCache implements Cache {
    public static final int DEFAULT_MAXIMUM_ENTRIES = 1024;

    private final com.github.benmanes.caffeine.cache.Cache<Object, QueryResult> cache;

    public CaffeineCache(int maximumSize, long defaultExprirationMinutes) {
        cache = Caffeine.newBuilder()
                .maximumSize(maximumSize)
                .expireAfterWrite(defaultExprirationMinutes, TimeUnit.MINUTES)
                .recordStats()
                .build();
    }

    @Override
    public QueryResult get(Object key) {
        return cache.getIfPresent(key);
    }

    @Override
    public void put(Object key, QueryResult result) {
        cache.put(key, result);
    }

    public com.github.benmanes.caffeine.cache.Cache<Object, QueryResult> getImplementation() {
        return cache;
    }
}
