/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.security.checks.prefab;

import com.yahoo.elide.security.User;
import com.yahoo.elide.security.checks.UserCheck;

/**
 * Simple checks to always grant or deny.
 */
public class Role {
    /**
     * Check which always grants.
     */
    public static class ALL extends UserCheck {
        @Override
        public boolean ok(User user) {
            return true;
        }
    }

    /**
     * Check which always denies.
     */
    public static class NONE extends UserCheck {
        @Override
        public boolean ok(User user) {
            return false;
        }
    }

    /**
     * Check which verifies if the user is a member of a particular role.
     */
    public static class RoleMemberCheck extends UserCheck {
        private String role;

        public RoleMemberCheck(String role) {
            this.role = role;
        }
        @Override
        public boolean ok(User user) {
            return user.isInRole(role);
        }
    }
}
