/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.optimization;

import com.yahoo.elide.security.User;

/**
 * Custom security access that verifies whether a user belongs to a role.
 * Permissions are assigned as a set of checks that grant access to the permission.
 *
 * NOTE: These checks are intended to be used for specific optimizations. That is, if the check
 * only requires user-level information and can cover an entire entity. These only apply to at
 * the entity-level. Field-level annotating of UserCheck's is not supported.
 */
public interface UserCheck {
    /**
     * Result of user level check.
     * ALLOW - Access to entire collection of resources
     * DENY - Access to none of these resources
     * FILTER - Filters collection by calling ok for each resource
     */
    enum UserPermission {
        ALLOW, DENY, FILTER
    }

    /* Helper defines */
    UserPermission ALLOW = UserPermission.ALLOW;
    UserPermission DENY = UserPermission.DENY;
    UserPermission FILTER = UserPermission.FILTER;

    /**
     * User level check to bypass need for per-record check.
     *
     * @param user per user check
     * @return FILTER to support ok check
     */
    UserPermission ok(User user);
}
