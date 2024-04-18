/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.request.route;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.paiondata.elide.core.dictionary.EntityDictionary;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Test for HeaderRouteResolver.
 */
class HeaderRouteResolverTest {

    @Test
    void headerApiVersion() {
        HeaderRouteResolver resolver = new HeaderRouteResolver("Accept-Version", "ApiVersion");
        Map<String, List<String>> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        headers.putAll(Map.of("ApiVersion", Arrays.asList("1")));
        Route route = resolver.resolve(null, null, null, headers, Collections.emptyMap());
        assertEquals("1", route.getApiVersion());
        assertSame(headers, route.getHeaders());
    }

    @Test
    void headerNoApiVersion() {
        HeaderRouteResolver resolver = new HeaderRouteResolver("Accept-Version", "ApiVersion");
        Map<String, List<String>> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        headers.putAll(Map.of("Test", Arrays.asList("1")));
        Route route = resolver.resolve(null, null, null, headers, Collections.emptyMap());
        assertEquals(EntityDictionary.NO_VERSION, route.getApiVersion());
        assertSame(headers, route.getHeaders());
    }
}
