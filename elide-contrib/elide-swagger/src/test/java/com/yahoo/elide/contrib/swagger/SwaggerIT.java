/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.swagger;

import com.yahoo.elide.contrib.swagger.resources.DocEndpoint;
import com.yahoo.elide.initialization.AbstractApiResourceInitializer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.restassured.RestAssured;

import org.testng.Assert;
import org.testng.annotations.Test;

public class SwaggerIT extends AbstractApiResourceInitializer {
    public SwaggerIT() {
        super(SwaggerResourceConfig.class, DocEndpoint.class.getPackage().getName());
    }

    @Test
    void testDocumentFetch() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(RestAssured.get("/doc/test").asString());
        Assert.assertNotNull(node.get("paths").get("/book"));
    }
}
