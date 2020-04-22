/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.integration.tests.framework;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
/**
 * HTTP Auth filter Implementation to use async query.
 */
public class AsyncAuthFilter implements ContainerRequestFilter {

    private static final String TEST_USER = "test";

    @Override
    public void filter(ContainerRequestContext request) {

        AsyncTestUser user = new AsyncTestUser();
        user.setName(TEST_USER);

        request.setSecurityContext(new AsyncTestSecurityContext(user));

    }
}
