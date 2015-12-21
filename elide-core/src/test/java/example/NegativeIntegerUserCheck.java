/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.optimization.UserCheck;
import com.yahoo.elide.security.Check;
import com.yahoo.elide.security.User;

import java.util.Optional;

/**
 * Useful for testing permissions based on different users.
 */
public class NegativeIntegerUserCheck implements UserCheck, Check<Object> {
    @Override
    public boolean ok(RequestScope requestScope, Optional optional) {
        Integer id = (Integer) requestScope.getUser().getOpaqueUser();
        return id >= 0;
    }

    @Override
    public UserPermission userPermission(User user) {
        Integer id = (Integer) user.getOpaqueUser();
        return id >= 0 ? ALLOW : DENY;
    }
}
