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
import com.yahoo.elide.security.checks.UserCheck;
import com.yahoo.elide.security.permissions.ExpressionResult;
import com.yahoo.elide.security.permissions.ExpressionResultCache;

import static com.yahoo.elide.security.permissions.ExpressionResult.DEFERRED;

/**
 * Special expression that only evaluates UserChecks.
 */
public class UserCheckOnlyExpression extends ImmediateCheckExpression {

    /**
     * Constructor.
     *
     * @param check Check
     * @param resource Persistent resource
     * @param requestScope Request scope
     * @param changeSpec ChangeSpec
     * @param cache Cache
     */
    public UserCheckOnlyExpression(final Check check,
                                   final PersistentResource resource,
                                   final RequestScope requestScope,
                                   final ChangeSpec changeSpec,
                                   final ExpressionResultCache cache) {
        super(check, resource, requestScope, changeSpec, cache);
    }

    @Override
    public ExpressionResult evaluate() {
        if (check instanceof UserCheck) {
            return super.evaluate();
        }
        result = DEFERRED;
        return result;
    }
}
