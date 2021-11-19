/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.core.security.checks;

import com.yahoo.elide.core.security.User;

import java.util.Optional;

/**
 * User check that allows the framework to override the implementation.
 */
public abstract class InjectableUserCheck extends UserCheck {
    @Override
    public boolean ok(User user) {
        if (getActualCheck().isPresent()) {
            return getActualCheck().get().ok((user));
        }

        return false;
    }

    public abstract Optional<UserCheck> getActualCheck();
}
