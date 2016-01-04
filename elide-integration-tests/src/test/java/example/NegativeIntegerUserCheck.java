/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.security.ChangeSpec;
import com.yahoo.elide.security.Check;
import com.yahoo.elide.security.User;
import com.yahoo.elide.optimization.UserCheck;

import java.util.Optional;

/**
 * Useful for testing permissions based on different users.
 */
public class NegativeIntegerUserCheck implements UserCheck, Check<Object> {
    @Override
    public UserPermission ok(User user) {
        Integer id = (Integer) user.getOpaqueUser();
        return id >= 0 ? ALLOW : DENY;
    }

    @Override
    public boolean ok(Object object, RequestScope requestScope, Optional<ChangeSpec> changeSpec) {
        return ok(requestScope, changeSpec);
    }

    @Override
    public boolean ok(RequestScope requestScope, Optional<ChangeSpec> changeSpec) {
        return ((Integer) requestScope.getUser().getOpaqueUser()) >= 0;
    }
}
