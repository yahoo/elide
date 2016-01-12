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
 * Strategy for determining whether a permission is allowed on a <i>specific</i> field.
 */
@Slf4j
public class SpecificField implements Strategy {

    @Override
    public void run(boolean hasPassingCheck, boolean hasDeferredCheck, boolean hasFieldChecks, boolean entityFailed) {
        // No field checks means default to entity-level
        // Otherwise, if nothing has succeeded and there are no commit checks to run, fail.
        // TODO: We can short-circuit this if we decide to pass in the check mode.
        if ((!hasFieldChecks && entityFailed) || (!hasDeferredCheck && !hasPassingCheck)) {
            log.debug("Final specific field check failed: {} {} {} {}",
                    hasPassingCheck, hasDeferredCheck, hasFieldChecks, entityFailed);
            throw new ForbiddenAccessException();
        }
    }

    @Override
    public boolean shouldContinueUponEntitySuccess(PermissionManager.CheckMode checkMode) {
        // Always continue since fields can override entity-level permissions
        log.debug("Entity-level check succeeded. Continuing to check for specific field overrides.");
        return true;
    }

    @Override
    public boolean shouldContinueUponFieldFailure(PermissionManager.CheckMode checkMode, boolean hasDeferredChecks) {
        // No need to wait if we either (a) require all checks to pass or (b) don't have deferred checks
        // to wait on
        if (!(checkMode == PermissionManager.CheckMode.ALL || !hasDeferredChecks)) {
            log.debug("Continuing on field-level check failure: {} {}", checkMode, hasDeferredChecks);
            return true;
        }
        log.debug("Not continuing on field-level check failure: {} {}", checkMode, hasDeferredChecks);
        return false;
    }
}
