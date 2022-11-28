/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import com.yahoo.elide.core.security.User;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.datastores.aggregation.query.Queryable;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.core.MultivaluedHashMap;

public class Slf4jQueryLoggerTest {

    @Test
    void testAccept() {
        Slf4jQueryLogger.Logger logger = (template, node) -> {
            assertEquals("QUERY ACCEPTED: {}", template);
            assertEquals("{\"id\":\"edc4a871-dff2-4054-804e-d80075cf828d\",\"user\":\"Unknown\",\"apiVersion\":\"1.0\",\"path\":\"/sales\",\"headers\":{\"Content-Type\":\"application/vnd.api+json\"}}", node.toString());
        };

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/vnd.api+json");

        Slf4jQueryLogger slf4jQueryLogger = new Slf4jQueryLogger(logger);
        slf4jQueryLogger.acceptQuery(
                UUID.fromString("edc4a871-dff2-4054-804e-d80075cf828d"),
                new User(null),
                headers,
                "1.0",
                new MultivaluedHashMap<>(),
                "/sales");
    }

    @Test
    void testProcess() {
        Slf4jQueryLogger.Logger logger = (template, node) -> {
            assertEquals("QUERY RUNNING: {}", template);
            assertEquals("{\"id\":\"edc4a871-dff2-4054-804e-d80075cf828d\",\"queries\":[\"foo\",\"bar\"],\"isCached\":true}", node.toString());
        };

        Slf4jQueryLogger slf4jQueryLogger = new Slf4jQueryLogger(logger);
        slf4jQueryLogger.processQuery(
                UUID.fromString("edc4a871-dff2-4054-804e-d80075cf828d"),
                Query.builder()
                        .source(mock(Queryable.class))
                        .build(),
                Arrays.asList("foo", "bar"),
                true);
    }

    @Test
    void testComplete() {
        Slf4jQueryLogger.Logger logger = (template, node) -> {
            assertEquals("QUERY COMPLETE: {}", template);
            assertEquals("{\"id\":\"edc4a871-dff2-4054-804e-d80075cf828d\",\"status\":200,\"error\":\"ok\"}", node.toString());
        };

        Slf4jQueryLogger slf4jQueryLogger = new Slf4jQueryLogger(logger);
        slf4jQueryLogger.completeQuery(
                UUID.fromString("edc4a871-dff2-4054-804e-d80075cf828d"),
                new QueryResponse(200, null, "ok"));
    }

    @Test
    void testCancel() {
        Slf4jQueryLogger.Logger logger = (template, node) -> {
            assertEquals("QUERY CANCELED: {}", template);
            assertEquals("{\"id\":\"edc4a871-dff2-4054-804e-d80075cf828d\"}", node.toString());
        };

        Slf4jQueryLogger slf4jQueryLogger = new Slf4jQueryLogger(logger);
        slf4jQueryLogger.cancelQuery(UUID.fromString("edc4a871-dff2-4054-804e-d80075cf828d"));
    }
}
