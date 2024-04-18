/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.request.route;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.paiondata.elide.core.dictionary.EntityDictionary;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Test for Route.
 */
class RouteTest {

    @Test
    void mutate() {
        Map<String, List<String>> headers = Map.of("Content-Type", Arrays.asList("application/json"));
        Route route = Route.builder().apiVersion("1").headers(headers).build();
        Route mutated = route.mutate().apiVersion("2").build();
        assertNotEquals(route.getApiVersion(), mutated.getApiVersion());
        assertEquals(route.getHeaders(), mutated.getHeaders());
        assertNotEquals(route, mutated);
    }

    @Test
    void parametersShouldNotBeNull() {
        assertNotNull(Route.builder().build().getParameters());
    }

    @Test
    void headersShouldNotBeNull() {
        assertNotNull(Route.builder().build().getHeaders());
    }

    @Test
    void baseUrlShouldNotBeNull() {
        assertNotNull(Route.builder().build().getBaseUrl());
    }

    @Test
    void pathShouldNotBeNull() {
        assertNotNull(Route.builder().build().getPath());
    }

    @Test
    void apiVersionShouldNotBeNull() {
        assertNotNull(Route.builder().build().getApiVersion());
    }

    @Test
    void apiVersionDefaultShouldBeNoVersion() {
        assertEquals(EntityDictionary.NO_VERSION, Route.builder().build().getApiVersion());
    }

    @Test
    void shouldEquals() {
        assertEquals(Route.builder().build(), Route.builder().build());
    }

    @Test
    void hashCodeShouldEquals() {
        assertEquals(Route.builder().build().hashCode(), Route.builder().build().hashCode());
    }

    @Test
    void toStringShouldEquals() {
        assertEquals(Route.builder().build().toString(), Route.builder().build().toString());
    }

    @Test
    void builderToStringShouldEquals() {
        assertEquals(Route.builder().toString(), Route.builder().toString());
    }
}
