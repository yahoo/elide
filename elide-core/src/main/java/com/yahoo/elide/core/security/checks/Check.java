/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.security.checks;
/**
 * Custom security access that verifies whether a user belongs to a role.
 * Permissions are assigned as a set of checks that grant access to the permission.
 */
public interface Check {

    /**
     * Should check run inline or be deferred to Commit
     */
    default boolean runInline() {
        return true;
    }
}
