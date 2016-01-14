/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.optimization.UserCheck;
import com.yahoo.elide.security.OperationCheck;
import com.yahoo.elide.security.User;

import java.util.Optional;

/**
 * Useful for testing permissions based on different users.
 */
public class NegativeIntegerUserCheck implements UserCheck, OperationCheck<Object> {
    @Override
    public boolean ok(Object object, RequestScope requestScope, Optional optional) {
        Integer id = (Integer) requestScope.getUser().getOpaqueUser();
        return id >= 0;
    }

    @Override
    public UserPermission ok(User user) {
        Integer id = (Integer) user.getOpaqueUser();
        return id >= 0 ? ALLOW : DENY;
    }
}
