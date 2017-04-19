/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.security.permissions.expressions;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.security.ChangeSpec;
import com.yahoo.elide.security.PersistentResource;
import com.yahoo.elide.security.RequestScope;
import com.yahoo.elide.security.checks.Check;
import com.yahoo.elide.security.checks.UserCheck;
import com.yahoo.elide.security.permissions.ExpressionResult;
import com.yahoo.elide.security.permissions.ExpressionResultCache;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

import static com.yahoo.elide.security.permissions.ExpressionResult.FAIL;
import static com.yahoo.elide.security.permissions.ExpressionResult.PASS;
import static com.yahoo.elide.security.permissions.ExpressionResult.UNEVALUATED;

/**
 * Expression for executing all specified checks.
 */
@Slf4j
public class ImmediateCheckExpression implements Expression {
    protected final Check check;
    protected final PersistentResource resource;
    protected final RequestScope requestScope;
    protected final ExpressionResultCache cache;
    protected ExpressionResult result;

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
        this.requestScope = requestScope;
        this.cache = cache;
        this.result = UNEVALUATED;

        // UserCheck does not use resource or changeSpec
        if (check instanceof UserCheck) {
            this.resource = null;
            this.changeSpec = Optional.empty();
        } else {
            this.resource = resource;
            this.changeSpec = Optional.ofNullable(changeSpec);
        }
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
        if (cache.hasStoredResultFor(checkClass, resource)) {
            result = cache.getResultFor(checkClass, resource);
        } else {
            result = computeCheck();
            cache.putResultFor(checkClass, resource, result);
            log.trace("-- Check computed result: {}", result);
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
        Object entity = (resource == null) ? null : resource.getObject();
        result = check.ok(entity, requestScope, changeSpec) ? PASS : FAIL;
        return result;
    }

    @Override
    public String toString() {
        EntityDictionary dictionary = ((com.yahoo.elide.core.RequestScope) requestScope).getDictionary();
        return String.format("(%s %s)", dictionary.getCheckIdentifier(check.getClass()), result);
    }
}
