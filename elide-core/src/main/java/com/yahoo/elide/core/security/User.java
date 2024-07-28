/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.security;

import java.security.Principal;
import java.util.Objects;

/**
 * The user associated with the request.
 */
public class User {
    private final Principal principal;

    /**
     * Constructor.
     *
     * @param principal the authenticated user
     */
    public User(Principal principal) {
        this.principal = principal;
    }

    /**
     * Gets the user principal of the authenticated user.
     *
     * @param <T> the type of principal
     * @return the principal
     */
    @SuppressWarnings("unchecked")
    public <T extends Principal> T getPrincipal() {
        return (T) this.principal;
    }

    /**
     * Gets the name of the authenticated user.
     *
     * @return the name of the authenticated user
     */
    public String getName() {
        return this.principal != null ? this.principal.getName() : null;
    }

    /**
     * Returns whether the authenticated user has the specified role.
     *
     * @param role the role to check for
     * @return true if the authenticated user has the role
     */
    public boolean isInRole(String role) {
        return false;
    }

    @Override
    public String toString() {
        return "User [principal=" + principal + "]";
    }

    @Override
    public int hashCode() {
        return Objects.hash(principal);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        User other = (User) obj;
        return Objects.equals(principal, other.principal);
    }
}
