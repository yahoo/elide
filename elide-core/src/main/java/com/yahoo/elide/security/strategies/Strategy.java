/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.security.strategies;

import com.yahoo.elide.security.PermissionManager;

/**
 * Interface to encode field-aware check result response.
 */
public interface Strategy {

    /**
     * Main method to calculate response for a field-aware check.
     *
     * In case of error, this method should throw a ForbiddenAccessException.
     *
     * @param hasPassingCheck Whether or not any check passed
     * @param hasDeferredCheck Whether or not there are still deferred checks to be run
     * @param hasFieldChecks Whether or not there were any field checks
     * @param entityFailed Whether or not the entity-level permission failed
     */
    void run(boolean hasPassingCheck, boolean hasDeferredCheck, boolean hasFieldChecks, boolean entityFailed);

    /**
     * Determine whether or not to continue checking in the event of a successful entity-level check.
     *
     * @param checkMode Check mode for the entity
     * @return True if should continue, false otherwise
     */
    boolean shouldContinueUponEntitySuccess(PermissionManager.CheckMode checkMode);

    /**
     * Determine whether or not to continue if encountering a field-level check failure.
     *
     * @param checkMode Check mode for field check
     * @param hasDeferredChecks Whether or not the are still deferred checks to be run
     */
    boolean shouldContinueUponFieldFailure(PermissionManager.CheckMode checkMode, boolean hasDeferredChecks);
}
