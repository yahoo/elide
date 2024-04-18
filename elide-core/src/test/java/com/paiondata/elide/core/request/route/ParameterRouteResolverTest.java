/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.request.route;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.paiondata.elide.core.dictionary.EntityDictionary;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Test for ParameterRouteResolver.
 */
class ParameterRouteResolverTest {

    @Test
    void parametersShouldPassThrough() {
        ParameterRouteResolver resolver = new ParameterRouteResolver("v", new BasicApiVersionValidator());
        Map<String, List<String>> parameters = Collections
                .unmodifiableMap(Map.of("Content-Type", Arrays.asList("application/json")));
        Route route = resolver.resolve(null, null, null, Collections.emptyMap(), parameters);
        assertSame(parameters, route.getParameters());
    }

    @Test
    void parametersRemoveApiVersion() {
        ParameterRouteResolver resolver = new ParameterRouteResolver("v", new BasicApiVersionValidator());
        Map<String, List<String>> parameters = Collections
                .unmodifiableMap(Map.of("Content-Type", Arrays.asList("application/json"), "v", Arrays.asList("1")));
        Route route = resolver.resolve(null, null, null, Collections.emptyMap(), parameters);
        assertNotSame(parameters, route.getParameters());
        assertNull(route.getParameters().get("v"));
        assertEquals("1", route.getApiVersion());
    }

    @Test
    void parametersInvalidApiVersion() {
        ParameterRouteResolver resolver = new ParameterRouteResolver("v", new BasicApiVersionValidator());
        Map<String, List<String>> parameters = Collections
                .unmodifiableMap(Map.of("Content-Type", Arrays.asList("application/json"), "v", Arrays.asList("abc")));
        Route route = resolver.resolve(null, null, null, Collections.emptyMap(), parameters);
        assertSame(parameters, route.getParameters());
        assertNotNull(route.getParameters().get("v"));
        assertEquals(EntityDictionary.NO_VERSION, route.getApiVersion());
    }
}
