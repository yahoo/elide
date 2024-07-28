/*
 * Copyright 2024, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.spring.security;

import com.yahoo.elide.core.security.User;

import jakarta.servlet.http.HttpServletRequest;

/**
 * {@link User} of the {@link HttpServletRequest}.
 * <p>
 * Spring Security will wrap the request using the
 * org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper.
 */
public class HttpServletRequestUser extends User {
    private final HttpServletRequest httpRequest;

    public HttpServletRequestUser(HttpServletRequest httpRequest) {
        super(httpRequest.getUserPrincipal());
        this.httpRequest = httpRequest;
    }

    @Override
    public boolean isInRole(String role) {
        return this.httpRequest.isUserInRole(role);
    }

    @Override
    public String getName() {
        String name = this.httpRequest.getRemoteUser();
        if (name == null) {
            name = super.getName();
        }
        return name;
    }
}
