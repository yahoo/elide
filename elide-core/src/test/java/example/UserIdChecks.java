/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.paiondata.elide.core.security.User;
import com.paiondata.elide.core.security.checks.UserCheck;

/**
 * Useful for testing permissions based on different users.
 */
public class UserIdChecks {

    public static class UserOneCheck extends UserCheck {
        @Override
        public boolean ok(User user) {
            Integer id = Integer.parseInt(user.getPrincipal().getName());
            return id.equals(1);
        }
    }

    public static class UserTwoCheck extends UserCheck {
        @Override
        public boolean ok(User user) {
            Integer id = Integer.parseInt(user.getPrincipal().getName());
            return id.equals(2);
        }
    }

    public static class UserThreeCheck extends UserCheck {
        @Override
        public boolean ok(User user) {
            Integer id = Integer.parseInt(user.getPrincipal().getName());
            return id.equals(3);
        }
    }

    public static class UserFourCheck extends UserCheck {
        @Override
        public boolean ok(User user) {
            Integer id = Integer.parseInt(user.getPrincipal().getName());
            return id.equals(4);
        }
    }
}
