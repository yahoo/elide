/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.jsonapi.serialization;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.yahoo.elide.jsonapi.models.JsonApiError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.Collections;
import java.util.Map;

/**
 * Test for JsonApiErrorSerializer.
 */
class JsonApiErrorSerializerTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    public void setup() {
        objectMapper = JsonMapper.builder().addModule(new JsonApiModule()).build();
    }

    @Test
    void idBlank() {
        JsonApiError jsonApiError = JsonApiError.builder().id("  ").build();
        String actual = objectMapper.writeValueAsString(jsonApiError);
        String expected = """
                {}""";
        assertEquals(expected, actual);
    }

    @Test
    void detailShouldBeEncoded() {
        JsonApiError jsonApiError = JsonApiError.builder().detail("<script></script>").build();
        String actual = objectMapper.writeValueAsString(jsonApiError);
        String expected = """
                {"detail":"&lt;script&gt;&lt;/script&gt;"}""";
        assertEquals(expected, actual);
    }

    @Test
    void detailBlank() {
        JsonApiError jsonApiError = JsonApiError.builder().detail("  ").build();
        String actual = objectMapper.writeValueAsString(jsonApiError);
        String expected = """
                {}""";
        assertEquals(expected, actual);
    }

    @Test
    void meta() {
        JsonApiError error = JsonApiError.builder()
                .meta(Map.of("property1", "value1"))
                .build();
        String actual = objectMapper.writeValueAsString(error);
        String expected = """
                {"meta":{"property1":"value1"}}""";
        assertEquals(expected, actual);
    }

    public static class Meta {
        public String property1 = "value1";
    }

    @Test
    void metaObject() {
        JsonApiError error = JsonApiError.builder()
                .meta(new Meta())
                .build();
        String actual = objectMapper.writeValueAsString(error);
        String expected = """
                {"meta":{"property1":"value1"}}""";
        assertEquals(expected, actual);
    }

    @Test
    void metaEmptyMap() {
        JsonApiError error = JsonApiError.builder()
                .meta(Collections.emptyMap())
                .build();
        String actual = objectMapper.writeValueAsString(error);
        String expected = """
                {}""";
        assertEquals(expected, actual);
    }

    @Test
    void links() {
        JsonApiError error = JsonApiError.builder()
                .links(links -> links.about("https://about").type("https://type"))
                .build();
        String actual = objectMapper.writeValueAsString(error);
        String expected = """
                {"links":{"about":"https://about","type":"https://type"}}""";
        assertEquals(expected, actual);
    }

    @Test
    void linksObject() {
        JsonApiError error = JsonApiError.builder()
                .links(JsonApiError.Links.builder().about("https://about").type("https://type").build())
                .build();
        String actual = objectMapper.writeValueAsString(error);
        String expected = """
                {"links":{"about":"https://about","type":"https://type"}}""";
        assertEquals(expected, actual);
    }

    @Test
    void source() {
        JsonApiError error = JsonApiError.builder()
                .source(source -> source.header("header").parameter("parameter").pointer("/data/attributes/title"))
                .build();
        String actual = objectMapper.writeValueAsString(error);
        String expected = """
                {"source":{"pointer":"/data/attributes/title","parameter":"parameter","header":"header"}}""";
        assertEquals(expected, actual);
    }

    @Test
    void sourceObject() {
        JsonApiError error = JsonApiError.builder()
                .source(JsonApiError.Source.builder().header("header").parameter("parameter")
                        .pointer("/data/attributes/title").build())
                .build();
        String actual = objectMapper.writeValueAsString(error);
        String expected = """
                {"source":{"pointer":"/data/attributes/title","parameter":"parameter","header":"header"}}""";
        assertEquals(expected, actual);
    }
}
