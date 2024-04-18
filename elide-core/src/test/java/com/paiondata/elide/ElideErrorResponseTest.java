/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Test for ElideErrorResponse.
 */
class ElideErrorResponseTest {

    @Test
    void okBody() {
        ElideErrorResponse<String> response = ElideErrorResponse.ok().body("test");
        assertEquals("test", response.getBody());
        assertEquals(200, response.getStatus());
    }

    @Test
    void ok() {
        ElideErrorResponse<String> response = ElideErrorResponse.ok("test");
        assertEquals("test", response.getBody());
        assertEquals(200, response.getStatus());
    }

    @Test
    void badRequestBody() {
        ElideErrorResponse<String> response = ElideErrorResponse.badRequest().body("test");
        assertEquals("test", response.getBody());
        assertEquals(400, response.getStatus());
    }

    @Test
    void badRequest() {
        ElideErrorResponse<String> response = ElideErrorResponse.badRequest("test");
        assertEquals("test", response.getBody());
        assertEquals(400, response.getStatus());
    }

    @Test
    void status() {
        ElideErrorResponse<Void> response = ElideErrorResponse.status(403).build();
        assertEquals(null, response.getBody());
        assertEquals(403, response.getStatus());
    }

    @Test
    void bodyCast() {
        ElideErrorResponse<String> response = ElideErrorResponse.badRequest("test");
        assertEquals(null, response.getBody(Integer.class));
        assertEquals("test", response.getBody(String.class));
        assertEquals(400, response.getStatus());
    }

    @Test
    void error() {
        ElideErrorResponse<ElideErrors> response = ElideErrorResponse.badRequest()
                .errors(errors -> errors.error(error -> error.message("Failed")));
        assertEquals("Failed", response.getBody().getErrors().get(0).getMessage());
        assertEquals(400, response.getStatus());
    }
}
