/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.request.route;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Tests for MediaTypeProfileRouteResolver.
 */
class MediaTypeProfileRouteResolverTest {

    @Test
    void testSingleShouldFind() {
        MediaTypeProfileRouteResolver resolver = new MediaTypeProfileRouteResolver("", new BasicApiVersionValidator(), () -> "");
        Map<String, List<String>> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        String mediaType = """
                application/vnd.api+json; profile=https://example.com/api/1
                """;

        headers.put("Accept", Collections.singletonList(mediaType));
        Route route = resolver.resolve("", null, "", headers, Collections.emptyMap());
        assertEquals("1", route.getApiVersion());
    }

    @Test
    void testShouldFind() {
        MediaTypeProfileRouteResolver resolver = new MediaTypeProfileRouteResolver("", new BasicApiVersionValidator(), () -> "");
        Map<String, List<String>> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        String mediaType = """
                application/vnd.api+json; ext="https://jsonapi.org/ext/version"; profile="https://example.com/resource-timestamps https://example.com/api/1"
                """;

        headers.put("Accept", Collections.singletonList(mediaType));
        Route route = resolver.resolve("", null, "", headers, Collections.emptyMap());
        assertEquals("1", route.getApiVersion());
    }

    @Test
    void testShouldNotFind() {
        MediaTypeProfileRouteResolver resolver = new MediaTypeProfileRouteResolver("", new BasicApiVersionValidator(), () -> "");
        Map<String, List<String>> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        String mediaType = """
                application/vnd.api+json; ext="https://jsonapi.org/ext/version"; profile="https://example.com/resource-timestamps https://example.com/api/"
                """;

        headers.put("Accept", Collections.singletonList(mediaType));
        Route route = resolver.resolve("", null, "", headers, Collections.emptyMap());
        assertEquals("", route.getApiVersion());
    }

    @Test
    void testVersionPrefixShouldFind() {
        MediaTypeProfileRouteResolver resolver = new MediaTypeProfileRouteResolver("v", new BasicApiVersionValidator(), () -> "");
        Map<String, List<String>> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        String mediaType = """
                application/vnd.api+json; ext="https://jsonapi.org/ext/version"; profile="https://example.com/resource-timestamps https://example.com/api/v1"
                """;

        headers.put("Accept", Collections.singletonList(mediaType));
        Route route = resolver.resolve("", null, "", headers, Collections.emptyMap());
        assertEquals("1", route.getApiVersion());
    }

    @Test
    void testVersionPrefixShouldNotFind() {
        MediaTypeProfileRouteResolver resolver = new MediaTypeProfileRouteResolver("v", new BasicApiVersionValidator(), () -> "");
        Map<String, List<String>> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        String mediaType = """
                application/vnd.api+json; ext="https://jsonapi.org/ext/version"; profile="https://example.com/resource-timestamps https://example.com/api/v"
                """;

        headers.put("Accept", Collections.singletonList(mediaType));
        Route route = resolver.resolve("", null, "", headers, Collections.emptyMap());
        assertEquals("", route.getApiVersion());
    }

    @Test
    void testUriPrefixShouldFind() {
        MediaTypeProfileRouteResolver resolver = new MediaTypeProfileRouteResolver("v", new BasicApiVersionValidator(), () -> "https://example.com/api");
        Map<String, List<String>> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        String mediaType = """
                application/vnd.api+json; ext="https://jsonapi.org/ext/version"; profile="https://example.com/resource-timestamps/v2 https://example.com/api/v1"
                """;

        headers.put("Accept", Collections.singletonList(mediaType));
        Route route = resolver.resolve("", null, "", headers, Collections.emptyMap());
        assertEquals("1", route.getApiVersion());
    }
}
