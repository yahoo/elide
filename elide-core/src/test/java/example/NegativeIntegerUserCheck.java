/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.security.User;
import com.yahoo.elide.security.checks.UserCheck;

/**
 * Useful for testing permissions based on different users.
 */
public class NegativeIntegerUserCheck extends UserCheck {
    @Override
    public boolean ok(User user) {
        Integer id = (Integer) user.getOpaqueUser();
        return id >= 0;
    }

    @Override
    public String checkIdentifier() {
        return "negativeIntegerUser";
    }
}
