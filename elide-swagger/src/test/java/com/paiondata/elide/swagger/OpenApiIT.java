/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.swagger;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.paiondata.elide.initialization.AbstractApiResourceInitializer;
import com.paiondata.elide.swagger.OpenApiDocument.MediaType;
import com.paiondata.elide.swagger.resources.ApiDocsEndpoint;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import org.junit.jupiter.api.Test;

class OpenApiIT extends AbstractApiResourceInitializer {
    public OpenApiIT() {
        super(ApiDocsResourceConfig.class, ApiDocsEndpoint.class.getPackage().getName());
    }

    @Test
    void testDocumentFetchJsonIndex() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(given().accept(MediaType.APPLICATION_JSON).get("/").asString());
        // Since there is only 1 document it is fetched
        assertTrue(node.get("paths").size() > 1);
    }

    @Test
    void testDocumentFetchYamlIndex() throws Exception {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        JsonNode node = mapper.readTree(given().accept(MediaType.APPLICATION_YAML).get("/").asString());
        // Since there is only 1 document it is fetched
        assertTrue(node.get("paths").size() > 1);
    }

    @Test
    void testDocumentFetchJson() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(given().accept(MediaType.APPLICATION_JSON).get("/test").asString());
        assertTrue(node.get("paths").size() > 1);
        assertNotNull(node.get("paths").get("/book"));
        assertNotNull(node.get("paths").get("/publisher"));
        assertNotNull(node.get("components").get("schemas").get("book"));
        assertNotNull(node.get("components").get("schemas").get("publisher"));
    }

    @Test
    void testVersion2DocumentFetchJson() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(
                given().accept(MediaType.APPLICATION_JSON).header("ApiVersion", "1.0").get("/test").asString());
        assertEquals(2, node.get("paths").size());
        assertNotNull(node.get("paths").get("/book"));
        assertNotNull(node.get("paths").get("/book/{bookId}"));
    }

    @Test
    void testVersion2DocumentFetchJsonPath() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(
                given().accept(MediaType.APPLICATION_JSON).get("/v1.0/test").asString());
        assertEquals(2, node.get("paths").size());
        assertNotNull(node.get("paths").get("/book"));
        assertNotNull(node.get("paths").get("/book/{bookId}"));
    }

    @Test
    void testDocumentFetchYaml() throws Exception {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        JsonNode node = mapper.readTree(given().accept(MediaType.APPLICATION_YAML).get("/test").asString());
        assertTrue(node.get("paths").size() > 1);
        assertNotNull(node.get("paths").get("/book"));
        assertNotNull(node.get("paths").get("/publisher"));
        assertNotNull(node.get("components").get("schemas").get("book"));
        assertNotNull(node.get("components").get("schemas").get("publisher"));
    }

    @Test
    void testVersion2DocumentFetchYaml() throws Exception {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        JsonNode node = mapper.readTree(
                given().accept(MediaType.APPLICATION_YAML).header("ApiVersion", "1.0").get("/test").asString());
        assertEquals(2, node.get("paths").size());
        assertNotNull(node.get("paths").get("/book"));
        assertNotNull(node.get("paths").get("/book/{bookId}"));
    }

    @Test
    void testUnknownVersionDocumentFetchYaml() throws Exception {
        given().accept(MediaType.APPLICATION_YAML).header("ApiVersion", "2.0").get("/test").then().statusCode(404);
    }
}
