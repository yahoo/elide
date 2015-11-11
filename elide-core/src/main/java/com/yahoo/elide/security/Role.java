/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.security;

/**
 * Simple checks to always grant or deny.
 */
public class Role {

    /**
     * Check which always grants.
     */
    public static class ALL implements UserCheck {
        @Override
        public UserPermission userPermission(User user) {
            return ALLOW;
        }
    }

    /**
     * Check which always denies.
     */
    public static class NONE implements UserCheck {
        @Override
        public UserPermission userPermission(User user) {
            return DENY;
        }
    }
}
