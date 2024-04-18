/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.core.security;

/**
 * Elide user for testing.
 */
public class TestUser extends User {

    public TestUser(String name) {
        super(() -> name);
    }
}
