/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.security.permissions.expressions;

import com.yahoo.elide.security.ChangeSpec;
import com.yahoo.elide.security.PersistentResource;
import com.yahoo.elide.security.RequestScope;
import com.yahoo.elide.security.checks.Check;
import com.yahoo.elide.security.checks.CommitCheck;
import com.yahoo.elide.security.permissions.ExpressionResult;
import com.yahoo.elide.security.permissions.ExpressionResultCache;
import lombok.extern.slf4j.Slf4j;

import static com.yahoo.elide.security.permissions.ExpressionResult.DEFERRED;

/**
 * Expression for only executing operation checks and skipping commit checks.
 */
@Slf4j
public class DeferredCheckExpression extends ImmediateCheckExpression {
    /**
     * Constructor.
     *
     * @param check Check
     * @param resource Persistent resource
     * @param requestScope Request scope
     * @param changeSpec Change spec
     * @param cache Cache
     */
    public DeferredCheckExpression(final Check check,
                                   final PersistentResource resource,
                                   final RequestScope requestScope,
                                   final ChangeSpec changeSpec,
                                   final ExpressionResultCache cache) {
        super(check, resource, requestScope, changeSpec, cache);
    }

    @Override
    public ExpressionResult evaluate() {
        if (check instanceof CommitCheck) {
            result = DEFERRED;
            log.debug("Deferring check: {}", check);
            return result;
        }
        return super.evaluate();
    }
}
