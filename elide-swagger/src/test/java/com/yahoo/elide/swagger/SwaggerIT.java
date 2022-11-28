/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.swagger;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yahoo.elide.initialization.AbstractApiResourceInitializer;
import com.yahoo.elide.swagger.resources.DocEndpoint;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

public class SwaggerIT extends AbstractApiResourceInitializer {
    public SwaggerIT() {
        super(SwaggerResourceConfig.class, DocEndpoint.class.getPackage().getName());
    }

    @Test
    void testDocumentFetch() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(get("/doc/test").asString());
        assertTrue(node.get("paths").size() > 1);
        assertNotNull(node.get("paths").get("/book"));
        assertNotNull(node.get("paths").get("/publisher"));
    }

    @Test
    void testVersion2DocumentFetch() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(given().header("ApiVersion", "1.0").get("/doc/test").asString());
        assertEquals(2, node.get("paths").size());
        assertNotNull(node.get("paths").get("/book"));
        assertNotNull(node.get("paths").get("/book/{bookId}"));
    }
}
