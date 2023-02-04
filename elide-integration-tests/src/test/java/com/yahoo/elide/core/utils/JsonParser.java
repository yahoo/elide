/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.utils;

import static org.junit.jupiter.api.Assertions.fail;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;

public class JsonParser {
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Parse provided string into JsonNode.
     *
     * @param jsonString provided JSON
     * @return JsonNode representation
     */
    public JsonNode toJsonNode(String jsonString) {
        try {
            return objectMapper.readTree(jsonString);
        } catch (IOException e) {
            fail("Unable to parse JSON\n" + jsonString, e);
            throw new IllegalStateException(); // should not reach here
        }
    }

    /**
     * Read resource as a JSON string.
     *
     * @param  resourceName name of the desired resource
     * @return JSON string
     */
    public String getJson(String resourceName) {
        try (InputStream is = this.getClass().getResourceAsStream(resourceName)) {
            return String.valueOf(objectMapper.readTree(is));
        } catch (IOException e) {
            fail("Unable to open test data " + resourceName, e);
            throw new IllegalStateException(); // should not reach here
        }
    }
}
