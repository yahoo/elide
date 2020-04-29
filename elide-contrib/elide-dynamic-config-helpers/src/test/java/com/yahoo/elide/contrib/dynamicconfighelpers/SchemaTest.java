/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.dynamicconfighelpers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hjson.JsonValue;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

public class SchemaTest {

    private InputStream loadStreamFromClasspath(String resource) throws Exception {
        return TableSchemaValidationTest.class.getResourceAsStream(resource);
    }

    private Reader loadReaderFromClasspath(String resource) throws Exception {
        return new InputStreamReader(loadStreamFromClasspath(resource));
    }

    protected JsonNode loadJsonFromClasspath(String resource, boolean translate) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();

        Reader reader = loadReaderFromClasspath(resource);

        if (translate) {
            String jsonText = JsonValue.readHjson(reader).toString();
            return objectMapper.readTree(jsonText);
        }

        return objectMapper.readTree(reader);
    }

    protected JsonNode loadJsonFromClasspath(String resource) throws Exception {
        return loadJsonFromClasspath(resource, false);
    }
}
