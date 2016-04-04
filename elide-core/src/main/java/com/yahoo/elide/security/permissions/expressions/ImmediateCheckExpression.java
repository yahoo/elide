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
import com.yahoo.elide.security.permissions.ExpressionResultCache;
import lombok.extern.slf4j.Slf4j;

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
    protected final ExpressionResultCache cache;

    private final Optional<ChangeSpec> changeSpec;

    /**
     * Constructor.
     *
     * @param check The check to be evaluated by this expression
     * @param resource The resource to pass to the check
     * @param requestScope The requestScope to pass to the check
     * @param changeSpec The changeSpec to pass to the check
     * @param cache The cache of previous expression results
     */
    public ImmediateCheckExpression(final Check check,
                                    final PersistentResource resource,
                                    final RequestScope requestScope,
                                    final ChangeSpec changeSpec,
                                    final ExpressionResultCache cache) {
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

        final ExpressionResult result;
        if (!cache.hasStoredResultFor(checkClass, resource)) {
            result = computeCheck();
            cache.putResultFor(checkClass, resource, result);
        } else {
            result = cache.getResultFor(checkClass, resource);
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

        com.yahoo.elide.core.RequestScope internalScope = (com.yahoo.elide.core.RequestScope) requestScope;
        if (resource == null) {
            internalScope.logAuthFailure(check.getClass());
        } else {
            internalScope.logAuthFailure(check.getClass(), resource.getType(), resource.getId());
        }

        return new ExpressionResult(FAIL, failure);
    }
}
