/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.security;

import com.yahoo.elide.optimization.UserCheck;
import com.yahoo.elide.optimization.UserCheck.UserPermission;

import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Wrapper for opaque user passed in every request. This user wrapper keeps track of
 * which checks have already been run (so they are not repeated.
 */
public class User {
    @Getter private final Object opaqueUser;
    private final Map<Class<? extends UserCheck>, UserPermission> okUserPermissions;

    public User(Object opaqueUser) {
        this.opaqueUser = opaqueUser;
        this.okUserPermissions = new LinkedHashMap<>();
    }

    /**
     * get UserPermission for provided check, cache result.
     *
     * @param check Check to run
     * @return UserPermission type
     */
    public UserPermission checkUserPermission(UserCheck check) {
        UserPermission checkType = okUserPermissions.get(check.getClass());
        if (checkType == null) {
            checkType = check.ok(this);
            okUserPermissions.put(check.getClass(), checkType);
        }
        return checkType;
    }
}
