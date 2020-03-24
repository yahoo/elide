package com.yahoo.elide.async.integration.framework;

import java.security.Principal;

import javax.ws.rs.core.SecurityContext;

public class AsyncTestSecurityContext implements SecurityContext {
    private AsyncTestUser user;

    public AsyncTestSecurityContext(AsyncTestUser user) {
        this.user = user;
    }

    @Override
    public String getAuthenticationScheme() {
        return SecurityContext.BASIC_AUTH;
    }

    @Override
    public Principal getUserPrincipal() {
        return this.user;
    }

    @Override
    public boolean isSecure() {
        return false;
    }

    @Override
    public boolean isUserInRole(String arg0) {
        return false;
    }

}