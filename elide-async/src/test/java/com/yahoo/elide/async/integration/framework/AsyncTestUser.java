package com.yahoo.elide.async.integration.framework;

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
