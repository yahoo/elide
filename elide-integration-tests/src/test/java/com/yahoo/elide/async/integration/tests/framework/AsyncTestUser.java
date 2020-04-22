/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.integration.tests.framework;

import java.security.Principal;

/**
 * Principal Implementation for Elide Standalone Example to use async query.
 */
public class AsyncTestUser implements Principal {
    private String name;

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return this.name;
    }
}
