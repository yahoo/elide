/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.security.permissions.expressions;

import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.security.ChangeSpec;
import com.yahoo.elide.security.checks.Check;
import com.yahoo.elide.security.permissions.ExpressionResult;

import static com.yahoo.elide.security.permissions.ExpressionResult.FAIL;
import static com.yahoo.elide.security.permissions.ExpressionResult.PASS;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Expression for executing all specified checks.
 */
public class ImmediateCheckExpression implements Expression {
    protected final Check check;
    private final PersistentResource resource;
    private final RequestScope requestScope;
    private final Optional<ChangeSpec> changeSpec;
    private final Map<Class<? extends Check>, Map<PersistentResource, ExpressionResult>> cache;

    /**
     * Constructor.
     *
     * @param check Check
     * @param resource Persistent resource
     * @param requestScope Request scope
     * @param changeSpec ChangeSpec
     * @param cache Cache
     */
    public ImmediateCheckExpression(final Check check,
                                    final PersistentResource resource,
                                    final RequestScope requestScope,
                                    final ChangeSpec changeSpec,
                                   final Map<Class<? extends Check>, Map<PersistentResource, ExpressionResult>> cache) {
        this.check = check;
        this.resource = resource;
        this.requestScope = requestScope;
        this.changeSpec = Optional.ofNullable(changeSpec);
        this.cache = cache;
    }

    @Override
    public ExpressionResult evaluate() {
        // If we have a valid change spec, do not cache the result or look for a cached result.
        if (changeSpec.isPresent()) {
            return computeCheck();
        }

        // Otherwise, search the cache and use value if found. Otherwise, evaluate and add it to the cache.
        Class<? extends Check> checkClass = check.getClass();
        Map<PersistentResource, ExpressionResult> resourceCache = cache.get(checkClass);
        if (resourceCache == null) {
            resourceCache = new IdentityHashMap<>();
            cache.put(checkClass, resourceCache);
        }

        if (!resourceCache.containsKey(resource)) {
            ExpressionResult result = computeCheck();
            resourceCache.put(resource, result);
            return result;
        }

        return resourceCache.get(resource);
    }

    /**
     * Actually compute the result of the check without caching concerns.
     *
     * @return Expression result from the check.
     */
    private ExpressionResult computeCheck() {
        if (resource == null) {
            return check.ok(null, requestScope, changeSpec) ? PASS : FAIL;
        }
        return check.ok(resource.getObject(), requestScope, changeSpec) ? PASS : FAIL;
    }
}
