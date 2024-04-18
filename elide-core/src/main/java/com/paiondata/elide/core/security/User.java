/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.security;

import lombok.Getter;

import java.security.Principal;

/**
 * Wrapper for opaque user passed in every request.
 */
public class User {
    @Getter private final Principal principal;

    public User(Principal principal) {
        this.principal = principal;
    }

    public String getName() {
        return principal != null ? principal.getName() : null;
    }

    public boolean isInRole(String role) {
        return false;
    }
}
