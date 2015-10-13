/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.security.User;
import com.yahoo.elide.security.UserCheck;

/**
 * Useful for testing permissions based on different users.
 */
public class NegativeIntegerUserCheck implements UserCheck {
    @Override
    public UserPermission userPermission(User user) {
        Integer id = (Integer) user.getOpaqueUser();
        return (id >= 0) ? ALLOW : DENY;
    }
}
