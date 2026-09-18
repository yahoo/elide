/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.core.security;

import jakarta.ws.rs.core.SecurityContext;

/**
 * Elide User for JAXRS.
 */
public class SecurityContextUser extends User {
    private SecurityContext ctx;

    public SecurityContextUser(SecurityContext ctx) {
        super(ctx.getUserPrincipal());
        this.ctx = ctx;
    }

    @Override
    public boolean isInRole(String role) {
        return ctx.isUserInRole(role);
    }
}
