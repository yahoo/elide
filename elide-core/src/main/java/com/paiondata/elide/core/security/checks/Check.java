/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.security.checks;
/**
 * Custom security access that verifies whether a user belongs to a role.
 * Permissions are assigned as a set of checks that grant access to the permission.
 */
public interface Check {

    /**
     * Should the check forced to be run at transaction commit or not.
     *
     * @return true to run at transaction commit
     */
    default boolean runAtCommit() {
        return false;
    }
}
