/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.request.route;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.paiondata.elide.core.dictionary.EntityDictionary;

import org.junit.jupiter.api.Test;

/**
 * Test for NullRouteResolver.
 */
class NullRouteResolverTest {

    @Test
    void shouldPassthrough() {
        NullRouteResolver routeResolver = new NullRouteResolver();
        Route route = routeResolver.resolve("", "baseUrl", "path", null, null);
        assertEquals("baseUrl", route.getBaseUrl());
        assertEquals("path", route.getPath());
        assertEquals(EntityDictionary.NO_VERSION, route.getApiVersion());
    }
}
