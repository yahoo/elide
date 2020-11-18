/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.security.checks;

import com.yahoo.elide.core.security.User;

/**
 * Custom security access that verifies whether a user belongs to a role.
 * Permissions are assigned as a set of checks that grant access to the permission.
 */
public abstract class UserCheck implements Check {
    /**
     * Method reserved for user checks.
     *
     * @param user User to check
     * @return True if user check passes, false otherwise
     */
    public abstract boolean ok(User user);
}
