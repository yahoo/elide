/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.jsonapi.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import lombok.Getter;

import java.util.Map;

/**
 * Test for JsonApiError.
 */
class JsonApiErrorTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void getMeta() throws JsonProcessingException {
        JsonApiError error = JsonApiError.builder()
                .meta(meta -> meta.put("property", "property"))
                .build();
        Map<String, Object> meta = error.getMeta();
        assertEquals("property", meta.get("property"));
    }

    @Test
    void meta() throws JsonProcessingException {
        JsonApiError error = JsonApiError.builder()
                .meta(Map.of("property1", "value1"))
                .build();
        String actual = objectMapper.writeValueAsString(error);
        String expected = """
                {"meta":{"property1":"value1"}}""";
        assertEquals(expected, actual);
    }


    @Test
    void links() throws JsonProcessingException {
        JsonApiError error = JsonApiError.builder()
                .links(links -> links.about("https://about").type("https://type"))
                .build();
        String actual = objectMapper.writeValueAsString(error);
        String expected = """
                {"links":{"about":"https://about","type":"https://type"}}""";
        assertEquals(expected, actual);
    }

    @Test
    void linksObject() throws JsonProcessingException {
        JsonApiError error = JsonApiError.builder()
                .links(JsonApiError.Links.builder().about("https://about").type("https://type").build())
                .build();
        String actual = objectMapper.writeValueAsString(error);
        String expected = """
                {"links":{"about":"https://about","type":"https://type"}}""";
        assertEquals(expected, actual);
    }

    @Test
    void source() throws JsonProcessingException {
        JsonApiError error = JsonApiError.builder()
                .source(source -> source.header("header").parameter("parameter").pointer("/data/attributes/title"))
                .build();
        String actual = objectMapper.writeValueAsString(error);
        String expected = """
                {"source":{"pointer":"/data/attributes/title","parameter":"parameter","header":"header"}}""";
        assertEquals(expected, actual);
    }

    @Test
    void sourceObject() throws JsonProcessingException {
        JsonApiError error = JsonApiError.builder()
                .source(JsonApiError.Source.builder().header("header").parameter("parameter")
                        .pointer("/data/attributes/title").build())
                .build();
        String actual = objectMapper.writeValueAsString(error);
        String expected = """
                {"source":{"pointer":"/data/attributes/title","parameter":"parameter","header":"header"}}""";
        assertEquals(expected, actual);
    }

    @Getter
    public static class MetaObject {
        String property = "value";
    }

    @Test
    void metaObject() throws JsonProcessingException {
        JsonApiError error = JsonApiError.builder()
                .meta(new MetaObject())
                .build();
        String actual = objectMapper.writeValueAsString(error);
        String expected = """
                {"meta":{"property":"value"}}""";
        assertEquals(expected, actual);
    }

    @Test
    void builderToStringEquals() {
        assertEquals(JsonApiError.builder().toString(), JsonApiError.builder().toString());
    }

    @Test
    void builderToStringNotEquals() {
        assertNotEquals(JsonApiError.builder().toString(), JsonApiError.builder().detail("detail").toString());
    }

    @Test
    void linksBuilderToStringEquals() {
        assertEquals(JsonApiError.Links.builder().toString(), JsonApiError.Links.builder().toString());
    }

    @Test
    void linksBuilderToStringNotEquals() {
        assertNotEquals(JsonApiError.Links.builder().toString(), JsonApiError.Links.builder().type("type").toString());
    }

    @Test
    void sourceBuilderToStringEquals() {
        assertEquals(JsonApiError.Source.builder().toString(), JsonApiError.Source.builder().toString());
    }

    @Test
    void sourceBuilderToStringNotEquals() {
        assertNotEquals(JsonApiError.Source.builder().toString(),
                JsonApiError.Source.builder().header("header").toString());
    }
}
