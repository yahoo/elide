/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.security;

import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.security.UserCheck.UserPermission;

import com.google.common.base.Preconditions;
import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Wrapper for opaque user passed in every request. This user wrapper keeps track of
 * which checks have already been run (so they are not repeated.
 */
public class User {
    private final @Getter Object opaqueUser;
    private final Map<Class<? extends Check>, UserPermission> okUserPermissions;
    private final Map<Class<? extends Check>, Set<PersistentResource>> okCheckResources;

    public User(Object opaqueUser) {
        this.opaqueUser = opaqueUser;
        this.okUserPermissions = new LinkedHashMap<>();
        this.okCheckResources = new LinkedHashMap<>();
    }

    /**
     * get UserPermission for provided check, cache result.
     *
     * @param check Check to run
     * @return UserPermission type
     */
    public UserPermission checkUserPermission(Check check) {
        if (check instanceof UserCheck) {
            UserPermission checkType = okUserPermissions.get(check.getClass());
            if (checkType == null) {
                checkType = ((UserCheck) check).userPermission(this);
                okUserPermissions.put(check.getClass(), checkType);
            }
            return checkType;
        }
        return UserPermission.FILTER;
    }

    /**
     * run filter check against provided resource and cache result.
     *
     * @param check Check to run
     * @param resource provided resource
     * @return true if allowed
     */
    public boolean ok(Check check, PersistentResource resource) {
        Preconditions.checkState(this == resource.getRequestScope().getUser());
        Set<PersistentResource> okResources;

        /* check user permission for ALLOW or DENY */
        switch (checkUserPermission(check)) {
            case ALLOW:
                return true;
            case DENY:
                return false;
            case FILTER:
                break;
        }

        /* check resource cache */
        if (okCheckResources.containsKey(check.getClass())) {
            okResources = okCheckResources.get(check.getClass());
            if (okResources.contains(resource)) {
                return true;
            }
        } else {
            okResources = new LinkedHashSet<>();
            okCheckResources.put(check.getClass(), okResources);
        }

        /* run check */
        boolean ok = check.ok(resource);

        if (ok) {
            okResources.add(resource);
        }
        return ok;
    }
}
