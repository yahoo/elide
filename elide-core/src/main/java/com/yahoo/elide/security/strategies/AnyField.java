/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.security.strategies;

import com.yahoo.elide.core.exceptions.ForbiddenAccessException;
import com.yahoo.elide.security.PermissionManager;
import lombok.extern.slf4j.Slf4j;

/**
 * A strategy for checking whether anything on the entity has permissions.
 */
@Slf4j
public class AnyField implements Strategy {

    @Override
    public boolean shouldContinueUponEntitySuccess(PermissionManager.CheckMode checkMode) {
        // If this is an "any" check, then we're done. If it is an "all" check, we may have commit checks queued
        // up. This means we really need to check additional fields and queue up those checks as well.
        log.debug("Succeeded for entity with checkmode: {}", checkMode);
        return PermissionManager.CheckMode.ANY != checkMode;
    }

    @Override
    public void run(boolean hasPassingCheck, boolean hasDeferredCheck, boolean hasFieldChecks, boolean entityFailed) {
        // If nothing succeeded, we know nothing is queued up. We should fail out.
        if (!hasPassingCheck && !hasDeferredCheck) {
            log.debug("Failed final check. No fields or entity-level checks passing: {}, {}, {}, {}",
                    hasPassingCheck, hasDeferredCheck, hasFieldChecks, entityFailed);
            throw new ForbiddenAccessException();
        }
    }

    @Override
    public boolean shouldContinueUponFieldFailure(PermissionManager.CheckMode checkMode, boolean hasDeferredChecks) {
        // Always keep searching in this case
        log.debug("Failed on a field. Continuing to check. CheckMode: {}, hasDeferred: {}",
                checkMode, hasDeferredChecks);
        return true;
    }
}
