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
 * @param <T> Type of record for Check
 */
public interface Check<T> {

    /**
     * Determines whether the user can access the resource.
     * @param record the record
     * @return true if allowed
     */
    public boolean ok(PersistentResource<T> record);
}
