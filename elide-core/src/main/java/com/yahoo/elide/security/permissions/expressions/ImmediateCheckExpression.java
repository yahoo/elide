/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.security.permissions.expressions;


import com.yahoo.elide.security.ChangeSpec;
import com.yahoo.elide.security.PersistentResource;
import com.yahoo.elide.security.RequestScope;
import com.yahoo.elide.security.SecurityMode;
import com.yahoo.elide.security.checks.Check;
import com.yahoo.elide.security.permissions.ExpressionResult;
import lombok.extern.slf4j.Slf4j;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;

import static com.yahoo.elide.security.permissions.ExpressionResult.PASS_RESULT;
import static com.yahoo.elide.security.permissions.ExpressionResult.Status.FAIL;

/**
 * Expression for executing all specified checks.
 */
@Slf4j
public class ImmediateCheckExpression implements Expression {
    protected final Check check;
    protected final PersistentResource resource;
    protected final RequestScope requestScope;
    protected final Map<Class<? extends Check>, Map<PersistentResource, ExpressionResult>> cache;

    private final Optional<ChangeSpec> changeSpec;

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
        log.trace("Evaluating check: {}", check);

        // If we have a valid change spec, do not cache the result or look for a cached result.
        if (changeSpec.isPresent()) {
            log.trace("-- Check has changespec: {}", changeSpec);
            ExpressionResult result = computeCheck();
            log.trace("-- Check returned with result: {}", result);
            return result;
        }

        // Otherwise, search the cache and use value if found. Otherwise, evaluate and add it to the cache.
        log.trace("-- Check does NOT have changespec");
        Class<? extends Check> checkClass = check.getClass();
        Map<PersistentResource, ExpressionResult> resourceCache = cache.get(checkClass);
        if (resourceCache == null) {
            resourceCache = new IdentityHashMap<>();
            cache.put(checkClass, resourceCache);
        }

        final ExpressionResult result;
        if (!resourceCache.containsKey(resource)) {
            result = computeCheck();
            resourceCache.put(resource, result);
        } else {
            result = resourceCache.get(resource);
        }

        log.trace("-- Check returned with result: {}", result);

        return result;
    }

    /**
     * Actually compute the result of the check without caching concerns.
     *
     * @return Expression result from the check.
     */
    private ExpressionResult computeCheck() {
        if (resource == null) {
            return check.ok(null, requestScope, changeSpec) ? PASS_RESULT : getFailureResult();
        }
        return check.ok(resource.getObject(), requestScope, changeSpec) ? PASS_RESULT : getFailureResult();
    }

    /**
     * Produce a failure result containing informative message.
     *
     * @return Expression result representing failure.
     */
    private ExpressionResult getFailureResult() {
        String failure = null;
        if (requestScope.getSecurityMode() == SecurityMode.SECURITY_ACTIVE_VERBOSE) {
            failure = "Check failed: "
                        + ((check == null) ? null : check.getClass().getName())
                        + " for object: "
                        + ((resource == null) ? "[resource was null-- user check?]" : resource.getObject());
        }
        if (resource == null) {
            ((com.yahoo.elide.core.RequestScope) requestScope).logAuthFailure(check.getClass());
        } else {
            ((com.yahoo.elide.core.RequestScope) requestScope).logAuthFailure(
                    check.getClass(), resource.getType(), resource.getId());
        }
        return new ExpressionResult(FAIL, failure);
    }
}
