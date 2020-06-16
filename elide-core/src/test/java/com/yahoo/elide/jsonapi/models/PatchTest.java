/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.jsonapi.models;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

public class PatchTest {
    @Test
    public void testSerialization() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode valueNode = mapper.readTree("\"stringValue\"");
        Patch patch = new Patch(Patch.Operation.ADD, "/foo/bar", valueNode);

        String expected = "{\"op\":\"add\",\"path\":\"/foo/bar\",\"value\":\"stringValue\"}";
        String actual = mapper.writeValueAsString(patch);

        assertEquals(expected, actual, "A patch object should serialize correctly as a string.");
    }

    @Test
     public void testDeserialization() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String input = "{\"op\":\"add\",\"path\":\"/foo/bar\",\"value\":\"stringValue\"}";
        Patch patch = mapper.readValue(input, Patch.class);

        assertEquals(Patch.Operation.ADD, patch.getOperation(), "Deserialized patch operation should match.");
        assertEquals("/foo/bar", patch.getPath(), "Deserialized patch path should match.");

        JsonNode node = patch.getValue();

        String value = mapper.treeToValue(node, String.class);

        assertEquals("stringValue", value, "Deserialized patch value should match");
    }

    @Test
    public void testListSerialization() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode valueNode = mapper.readTree("\"stringValue\"");
        List<Patch> patches = Collections.singletonList(new Patch(Patch.Operation.ADD, "/foo/bar", valueNode));

        String expected = "[{\"op\":\"add\",\"path\":\"/foo/bar\",\"value\":\"stringValue\"}]";
        String actual = mapper.writeValueAsString(patches);

        assertEquals(expected, actual, "A list of patch object should serialized correctly as a string.");
    }

    @Test
    public void testListDeserialization() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        String input = "[{\"op\":\"add\",\"path\":\"/foo/bar\",\"value\":\"stringValue\"}]";

        List<Patch> patches = mapper.readValue(input, new TypeReference<List<Patch>>() { });
        assertEquals(Patch.Operation.ADD, patches.get(0).getOperation(), "Deserialized patch operation should match.");
        assertEquals("/foo/bar", patches.get(0).getPath(), "Deserialized patch path should match.");

        JsonNode node = patches.get(0).getValue();

        String value = mapper.treeToValue(node, String.class);

        assertEquals("stringValue", value, "Deserialized patch value should match");
    }
}
