/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.jsonapi.serialization;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.paiondata.elide.jsonapi.models.JsonApiError;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

/**
 * Test for JsonApiErrorSerializer.
 */
class JsonApiErrorSerializerTest {

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void setup() {
        objectMapper.registerModule(new JsonApiModule());
    }

    @Test
    void idBlank() throws JsonProcessingException {
        JsonApiError jsonApiError = JsonApiError.builder().id("  ").build();
        String actual = objectMapper.writeValueAsString(jsonApiError);
        String expected = """
                {}""";
        assertEquals(expected, actual);
    }

    @Test
    void detailShouldBeEncoded() throws JsonProcessingException {
        JsonApiError jsonApiError = JsonApiError.builder().detail("<script></script>").build();
        String actual = objectMapper.writeValueAsString(jsonApiError);
        String expected = """
                {"detail":"&lt;script&gt;&lt;/script&gt;"}""";
        assertEquals(expected, actual);
    }

    @Test
    void detailBlank() throws JsonProcessingException {
        JsonApiError jsonApiError = JsonApiError.builder().detail("  ").build();
        String actual = objectMapper.writeValueAsString(jsonApiError);
        String expected = """
                {}""";
        assertEquals(expected, actual);
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

    public static class Meta {
        public String property1 = "value1";
    }

    @Test
    void metaObject() throws JsonProcessingException {
        JsonApiError error = JsonApiError.builder()
                .meta(new Meta())
                .build();
        String actual = objectMapper.writeValueAsString(error);
        String expected = """
                {"meta":{"property1":"value1"}}""";
        assertEquals(expected, actual);
    }

    @Test
    void metaEmptyMap() throws JsonProcessingException {
        JsonApiError error = JsonApiError.builder()
                .meta(Collections.emptyMap())
                .build();
        String actual = objectMapper.writeValueAsString(error);
        String expected = """
                {}""";
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
}
