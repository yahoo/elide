/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.jsonapi.models;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.testng.Assert;
import org.testng.annotations.Test;
import org.testng.collections.Lists;

import java.util.List;

public class PatchTest {
    @Test
    public void testSerialization() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode valueNode = mapper.readTree("\"stringValue\"");
        Patch patch = new Patch(Patch.Operation.ADD, "/foo/bar", valueNode);

        String expected = "{\"op\":\"add\",\"path\":\"/foo/bar\",\"value\":\"stringValue\"}";
        String actual = mapper.writeValueAsString(patch);

        Assert.assertEquals(expected, actual, "A patch object should serialize correctly as a string.");
    }

    @Test
     public void testDeserialization() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String input = "{\"op\":\"add\",\"path\":\"/foo/bar\",\"value\":\"stringValue\"}";
        Patch patch = mapper.readValue(input, Patch.class);

        Assert.assertEquals(patch.getOperation(), Patch.Operation.ADD, "Deserialized patch operation should match.");
        Assert.assertEquals(patch.getPath(), "/foo/bar", "Deserialized patch path should match.");

        JsonNode node = patch.getValue();

        String value = mapper.treeToValue(node, String.class);

        Assert.assertEquals(value, "stringValue", "Deserialized patch value should match");
    }

    @Test
    public void testListSerialization() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode valueNode = mapper.readTree("\"stringValue\"");
        List<Patch> patches = Lists.newArrayList(new Patch(Patch.Operation.ADD, "/foo/bar", valueNode));

        String expected = "[{\"op\":\"add\",\"path\":\"/foo/bar\",\"value\":\"stringValue\"}]";
        String actual = mapper.writeValueAsString(patches);

        Assert.assertEquals(expected, actual, "A list of patch object should serialized correctly as a string.");
    }

    @Test
    public void testListDeserialization() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        String input = "[{\"op\":\"add\",\"path\":\"/foo/bar\",\"value\":\"stringValue\"}]";

        List<Patch> patches = mapper.readValue(input, new TypeReference<List<Patch>>() { });
        Assert.assertEquals(patches.get(0).getOperation(), Patch.Operation.ADD, "Deserialized patch operation should match.");
        Assert.assertEquals(patches.get(0).getPath(), "/foo/bar", "Deserialized patch path should match.");

        JsonNode node = patches.get(0).getValue();

        String value = mapper.treeToValue(node, String.class);

        Assert.assertEquals(value, "stringValue", "Deserialized patch value should match");
    }
}
