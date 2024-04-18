/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.spring.security;

import com.paiondata.elide.core.security.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

/**
 * Elide User object for Spring Boot.
 */
public class AuthenticationUser extends User {
    private Authentication authentication;

    public AuthenticationUser(Authentication authentication) {
        super(authentication);
        this.authentication = authentication;
    }

    @Override
    public boolean isInRole(String role) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role::equals);
    }
}
