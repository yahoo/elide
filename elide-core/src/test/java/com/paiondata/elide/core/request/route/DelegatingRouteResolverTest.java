/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.request.route;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

/**
 * Test for DelegatingRouteResolver.
 */
class DelegatingRouteResolverTest {

    @Test
    void shouldDelegate() {
        RouteResolver delegate = mock(RouteResolver.class);
        DelegatingRouteResolver routeResolver = new DelegatingRouteResolver(delegate);
        when(delegate.resolve("", "", "", null, null)).thenReturn(Route.builder().apiVersion("2").build());
        Route route = routeResolver.resolve("", "", "", null, null);
        assertEquals("2", route.getApiVersion());
    }

    @Test
    void shouldPassthrough() {
        RouteResolver delegate = mock(RouteResolver.class);
        DelegatingRouteResolver routeResolver = new DelegatingRouteResolver(delegate);
        when(delegate.resolve("", "baseUrl", "", null, null)).thenReturn(Route.builder().apiVersion("").baseUrl("test").build());
        Route route = routeResolver.resolve("", "baseUrl", "", null, null);
        assertEquals("baseUrl", route.getBaseUrl());
    }
}
