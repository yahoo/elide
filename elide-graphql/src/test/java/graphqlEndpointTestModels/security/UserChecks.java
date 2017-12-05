/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package graphqlEndpointTestModels.security;

import com.yahoo.elide.security.User;
import com.yahoo.elide.security.checks.UserCheck;

import java.security.Principal;

public class UserChecks {
    public static final String IS_USER_1 = "is user one";
    public static final String IS_USER_2 = "is user two";

    public abstract static class IsUserId {
        public static class One extends UserCheck {
            @Override
            public boolean ok(User user) {
                Principal principal = (Principal) user.getOpaqueUser();
                if (principal == null) {
                    return false;
                }
                return "1".equals(principal.getName());
            }
        }

        public static class Two extends UserCheck {
            @Override
            public boolean ok(User user) {
                Principal principal = (Principal) user.getOpaqueUser();
                if (principal == null) {
                    return false;
                }
                return "2".equals(principal.getName());
            }
        }
    }
}
