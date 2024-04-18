/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.paiondata.elide.core.security.User;
import com.paiondata.elide.core.security.checks.UserCheck;

/**
 * Useful for testing permissions based on different users.
 */
public class NegativeIntegerUserCheck extends UserCheck {
    @Override
    public boolean ok(User user) {
        Integer id = Integer.parseInt(user.getPrincipal().getName());
        return id >= 0;
    }
}
