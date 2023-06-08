/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.request.route;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

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
    }
}
