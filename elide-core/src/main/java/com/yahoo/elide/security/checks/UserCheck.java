/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.security.checks;

import com.yahoo.elide.core.RequestScope;

import java.util.Optional;

/**
 * Custom security access that verifies whether a user belongs to a role.
 * Permissions are assigned as a set of checks that grant access to the permission.
 */
public abstract class UserCheck extends InlineCheck {
    /* NOTE: Operation checks and user checks are intended to be _distinct_ */
    @Override
    public final boolean ok(Object object, RequestScope requestScope, Optional optional) {
        System.out.print("\n\n\n\n\n\n\n\nsdfdfdsfsdfd");
        return ok(requestScope.getUser());
    }
}
