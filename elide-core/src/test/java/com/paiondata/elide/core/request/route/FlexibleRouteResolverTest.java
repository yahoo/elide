/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.request.route;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Test for FlexibleRouteResolver.
 */
class FlexibleRouteResolverTest {

    @Test
    void parametersApiVersion() {
        FlexibleRouteResolver resolver = new FlexibleRouteResolver(new BasicApiVersionValidator(), () -> "");
        Map<String, List<String>> parameters = Collections
                .unmodifiableMap(Map.of("v", Arrays.asList("1")));
        Route route = resolver.resolve("", "", "", Collections.emptyMap(), parameters);
        assertNotSame(parameters, route.getParameters());
        assertEquals("1", route.getApiVersion());
    }

    @Test
    void pathApiVersion() {
        FlexibleRouteResolver resolver = new FlexibleRouteResolver(new BasicApiVersionValidator(), () -> "");
        Route route = resolver.resolve("", "", "/v1/books", Collections.emptyMap(), Collections.emptyMap());
        assertEquals("1", route.getApiVersion());
        assertEquals("/v1", route.getBaseUrl());
        assertEquals("books", route.getPath());
    }

    @Test
    void headerApiVersion() {
        FlexibleRouteResolver resolver = new FlexibleRouteResolver(new BasicApiVersionValidator(), () -> "");
        Map<String, List<String>> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        headers.putAll(Map.of("ApiVersion", Arrays.asList("1")));
        Route route = resolver.resolve("", "", "", headers, Collections.emptyMap());
        assertEquals("1", route.getApiVersion());
        assertSame(headers, route.getHeaders());
    }

    @Test
    void headerAcceptVersion() {
        FlexibleRouteResolver resolver = new FlexibleRouteResolver(new BasicApiVersionValidator(), () -> "");
        Map<String, List<String>> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        headers.putAll(Map.of("Accept-Version", Arrays.asList("1")));
        Route route = resolver.resolve("", "", "", headers, Collections.emptyMap());
        assertEquals("1", route.getApiVersion());
        assertSame(headers, route.getHeaders());
    }

    @Test
    void mediaTypeProfileApiVersion() {
        FlexibleRouteResolver resolver = new FlexibleRouteResolver(new BasicApiVersionValidator(), () -> "");
        Map<String, List<String>> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        String mediaType = """
                application/vnd.api+json; profile=https://example.com/api/v1
                """;

        headers.put("Accept", Arrays.asList(mediaType));
        Route route = resolver.resolve("", "", "", headers, Collections.emptyMap());
        assertEquals("1", route.getApiVersion());
    }
}
