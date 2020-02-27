/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.security;

import java.security.Principal;

public class TestUser extends User {

    public TestUser(String name) {
        super(new Principal() {
            @Override
            public String getName() {
                return name;
            }
        });
    }
}
