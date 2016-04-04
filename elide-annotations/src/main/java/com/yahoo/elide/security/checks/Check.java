/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.security.checks;

import com.yahoo.elide.security.ChangeSpec;
import com.yahoo.elide.security.RequestScope;
import com.yahoo.elide.security.User;

import java.util.Optional;

/**
 * Custom security access that verifies whether a user belongs to a role.
 * Permissions are assigned as a set of checks that grant access to the permission.
 * @param <T> Type of record for Check
 */
public interface Check<T> {

    /**
     * Determines whether the user can access the resource.
     *
     * @param object Fully modified object
     * @param requestScope Request scope object
     * @param changeSpec Summary of modifications
     * @return true if security check passed
     */
    boolean ok(T object, RequestScope requestScope, Optional<ChangeSpec> changeSpec);

    /**
     * Method reserved for user checks.
     *
     * @param user User to check
     * @return True if user check passes, false otherwise
     */
    boolean ok(User user);

    default String checkIdentifier() {
        return this.getClass().getName();
    }
}
