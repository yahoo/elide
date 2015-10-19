/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.security;

import com.yahoo.elide.core.PersistentResource;

/**
 * Custom security access that verifies whether a user belongs to a role.
 * Permissions are assigned as a set of checks that grant access to the permission.
 *
 * @param <T> Type of record for Check
 */
public interface UserCheck<T> extends Check<T> {
    /**
     * Result of user level check.
     * ALLOW - Access to entire collection of resources
     * DENY - Access to none of these resources
     * FILTER - Filters collection by calling ok for each resource
     */
    static enum UserPermission {
        ALLOW, DENY, FILTER
    }

    /* Helper defines */
    final static UserPermission ALLOW = UserPermission.ALLOW;
    final static UserPermission DENY = UserPermission.DENY;
    final static UserPermission FILTER = UserPermission.FILTER;

    /**
     * Determines whether the user can access the resource.
     * Must be defined when userPermission == FILTER
     *
     * @param record the record
     * @return true if allowed
     */
    @Override
    default boolean ok(PersistentResource<T> record) {
        throw new UnsupportedOperationException();
    }

    /**
     * User level check to bypass need for per-record check.
     *
     * @param user per user check
     * @return FILTER to support ok check
     */
    UserPermission userPermission(User user);
}
