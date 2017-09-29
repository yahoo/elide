/*
 * Copyright 2017, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.security.checks.UserCheck;
import com.yahoo.elide.security.User;

/**
 * Useful for testing permissions based on different users.
 */
public class UserIdChecks {

    public static class UserOneCheck extends UserCheck {
        @Override
        public boolean ok(User user) {
            Integer id = (Integer) user.getOpaqueUser();
            return id.equals(1);
        }

        @Override
        public String checkIdentifier() {
            return "UserOne";
        }
    }

    public static class UserTwoCheck extends UserCheck {
        @Override
        public boolean ok(User user) {
            Integer id = (Integer) user.getOpaqueUser();
            return id.equals(2);
        }

        @Override
        public String checkIdentifier() {
            return "UserTwo";
        }
    }

    public static class UserThreeCheck extends UserCheck {
        @Override
        public boolean ok(User user) {
            Integer id = (Integer) user.getOpaqueUser();
            return id.equals(3);
        }

        @Override
        public String checkIdentifier() {
            return "UserTwo";
        }
    }
}
