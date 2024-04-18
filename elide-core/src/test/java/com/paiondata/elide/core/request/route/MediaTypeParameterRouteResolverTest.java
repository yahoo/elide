/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.request.route;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.paiondata.elide.core.dictionary.EntityDictionary;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Test for MediaTypeParameterRouteResolver.
 */
class MediaTypeParameterRouteResolverTest {

    @Test
    void version() {
        Map<String, List<String>> headers = Collections
                .unmodifiableMap(Map.of("accept", Arrays.asList("application/json; v=1")));

        MediaTypeParameterRouteResolver resolver = new MediaTypeParameterRouteResolver(parameter -> {
            if (parameter.startsWith("v=")) {
                return parameter.substring("v=".length());
            }
            return null;
        });
        Route route = resolver.resolve("application/json", null, null, headers, null);
        assertEquals("1", route.getApiVersion());
    }

    @Test
    void noVersion() {
        Map<String, List<String>> headers = Collections
                .unmodifiableMap(Map.of("accept", Arrays.asList("application/json")));

        MediaTypeParameterRouteResolver resolver = new MediaTypeParameterRouteResolver(parameter -> {
            if (parameter.startsWith("v=")) {
                return parameter.substring("v=".length());
            }
            return null;
        });
        Route route = resolver.resolve("application/json", null, null, headers, null);
        assertEquals(EntityDictionary.NO_VERSION, route.getApiVersion());
    }
}
