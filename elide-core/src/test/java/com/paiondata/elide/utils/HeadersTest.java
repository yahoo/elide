/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.utils;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

/**
 * Test for headers.
 */
class HeadersTest {

    @Test
    void removeAuthorizationHeadersShouldNotCopyUnnecessarily() {
        Map<String, List<String>> headers = Map.of("Content-Type", List.of("application/json"));
        Map<String, List<String>> processed = Headers.removeAuthorizationHeaders(headers);
        assertSame(headers, processed);
    }

    @Test
    void removeAuthorizationHeadersProcessedShouldBeCaseInsensitive() {
        Map<String, List<String>> headers = Map.of("Content-Type", List.of("application/json"), "Authorization", List.of("Bearer"));
        Map<String, List<String>> processed = Headers.removeAuthorizationHeaders(headers);
        assertNotNull(processed.get("content-type"));
    }
}
