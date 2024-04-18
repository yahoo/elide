/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Test for ElideResponse.
 */
class ElideResponseTest {

    @Test
    void okBody() {
        ElideResponse<String> response = ElideResponse.ok().body("test");
        assertEquals("test", response.getBody());
        assertEquals(200, response.getStatus());
    }

    @Test
    void ok() {
        ElideResponse<String> response = ElideResponse.ok("test");
        assertEquals("test", response.getBody());
        assertEquals(200, response.getStatus());
    }

    @Test
    void badRequestBody() {
        ElideResponse<String> response = ElideResponse.badRequest().body("test");
        assertEquals("test", response.getBody());
        assertEquals(400, response.getStatus());
    }

    @Test
    void badRequest() {
        ElideResponse<String> response = ElideResponse.badRequest("test");
        assertEquals("test", response.getBody());
        assertEquals(400, response.getStatus());
    }

    @Test
    void status() {
        ElideResponse<Void> response = ElideResponse.status(403).build();
        assertEquals(null, response.getBody());
        assertEquals(403, response.getStatus());
    }

    @Test
    void bodyCast() {
        ElideResponse<String> response = ElideResponse.badRequest("test");
        assertEquals(null, response.getBody(Integer.class));
        assertEquals("test", response.getBody(String.class));
        assertEquals(400, response.getStatus());
    }
}
