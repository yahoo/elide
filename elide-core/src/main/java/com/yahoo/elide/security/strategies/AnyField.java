/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.security.strategies;

import com.yahoo.elide.core.exceptions.ForbiddenAccessException;
import com.yahoo.elide.security.PermissionManager;

/**
 * A strategy for checking whether anything on the entity has permissions.
 */
public class AnyField implements Strategy {

    @Override
    public boolean shouldContinueUponEntitySuccess(PermissionManager.CheckMode checkMode) {
        // If this is an "any" check, then we're done. If it is an "all" check, we may have commit checks queued
        // up. This means we really need to check additional fields and queue up those checks as well.
        return PermissionManager.CheckMode.ANY != checkMode;
    }

    @Override
    public void run(boolean hasPassingCheck, boolean hasDeferredCheck, boolean hasFieldChecks, boolean entityFailed) {
        // If nothing succeeded, we know nothing is queued up. We should fail out.
        if (!hasPassingCheck && !hasDeferredCheck) {
            throw new ForbiddenAccessException();
        }
    }

    @Override
    public boolean shouldContinueUponFieldFailure(PermissionManager.CheckMode checkMode, boolean hasDeferredChecks) {
        // Always keep searching in this case
        return true;
    }
}
