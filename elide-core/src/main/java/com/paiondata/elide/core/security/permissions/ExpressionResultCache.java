/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.security.permissions;

import com.paiondata.elide.core.security.PersistentResource;
import com.paiondata.elide.core.security.checks.Check;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Used to cache the results of checks so that if a check is not evaluated more than once for a given resource.
 */
public class ExpressionResultCache {
    private final Map<Class<? extends Check>, Map<PersistentResource, ExpressionResult>> computedResults;

    public ExpressionResultCache() {
        computedResults = new HashMap<>();
    }


    public boolean hasStoredResultFor(Class<? extends Check> checkClass, PersistentResource resource) {
        return computedResults.containsKey(checkClass)
                && computedResults.get(checkClass).containsKey(resource);
    }

    public void putResultFor(Class<? extends Check> checkClass, PersistentResource resource, ExpressionResult result) {
        Map<PersistentResource, ExpressionResult> cache = computedResults.computeIfAbsent(checkClass,
                unused -> new IdentityHashMap<>());
        cache.put(resource, result);
    }

    public ExpressionResult getResultFor(Class<? extends Check> checkClass, PersistentResource resource) {
        return computedResults.get(checkClass).get(resource);
    }
}
