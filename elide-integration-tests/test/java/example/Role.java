/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.security.User;
import com.yahoo.elide.security.UserCheck;

/**
 * Example UserCheck beans
 */
public class Role {
    /** Allow check */
    public static class ALL implements UserCheck {
        @Override
        public UserPermission userPermission(User user) {
            return ALLOW;
        }
    }
    /** Deny check */
    public static class NONE implements UserCheck {
        @Override
        public UserPermission userPermission(User user) {
            return DENY;
        }
    }
}
