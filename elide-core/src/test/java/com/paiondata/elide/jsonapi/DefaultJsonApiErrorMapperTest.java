/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.jsonapi;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.paiondata.elide.ElideError;
import com.paiondata.elide.jsonapi.models.JsonApiError;
import com.paiondata.elide.jsonapi.models.JsonApiError.Links;
import com.paiondata.elide.jsonapi.models.JsonApiError.Source;
import com.paiondata.elide.jsonapi.serialization.JsonApiModule;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

/**
 * Test for DefaultJsonApiErrorMapper.
 */
class DefaultJsonApiErrorMapperTest {

    private ObjectMapper objectMapper = new ObjectMapper();
    private JsonApiErrorMapper mapper = new DefaultJsonApiErrorMapper();

    @BeforeEach
    public void setup() {
        objectMapper.registerModule(new JsonApiModule());
    }

    @Test
    void toJsonApiError() throws JsonProcessingException {
        JsonApiError jsonApiError = mapper
                .toJsonApiError(ElideError.builder()
                        .message("<script>message</script>")
                        .attribute("id", "id")
                        .attribute("status", "status")
                        .attribute("code", "code")
                        .attribute("title", "title")
                        .attribute("source", Source.builder().header("header").build())
                        .attribute("links", Links.builder().about("about").build())
                        .build());
        String actual = objectMapper.writeValueAsString(jsonApiError);
        String expected = """
                {"id":"id","links":{"about":"about"},"status":"status","code":"code","source":{"header":"header"},"title":"title","detail":"&lt;script&gt;message&lt;/script&gt;"}""";
        assertEquals(expected, actual);
    }

    @Test
    void toJsonApiErrorMeta() throws JsonProcessingException {
        JsonApiError jsonApiError = mapper
                .toJsonApiError(ElideError.builder()
                        .message("message")
                        .attribute("property", "property")
                        .build());
        String actual = objectMapper.writeValueAsString(jsonApiError);
        String expected = """
                {"detail":"message","meta":{"property":"property"}}""";
        assertEquals(expected, actual);
    }

    @Test
    void toJsonApiErrorLinks() throws JsonProcessingException {
        JsonApiError jsonApiError = mapper
                .toJsonApiError(ElideError.builder()
                        .message("message")
                        .attribute("links", Map.of("about", "about"))
                        .build());
        String actual = objectMapper.writeValueAsString(jsonApiError);
        String expected = """
                {"links":{"about":"about"},"detail":"message"}""";
        assertEquals(expected, actual);
    }

    @Test
    void toJsonApiErrorSource() throws JsonProcessingException {
        JsonApiError jsonApiError = mapper
                .toJsonApiError(ElideError.builder()
                        .message("message")
                        .attribute("source", Map.of("pointer", "pointer"))
                        .build());
        String actual = objectMapper.writeValueAsString(jsonApiError);
        String expected = """
                {"source":{"pointer":"pointer"},"detail":"message"}""";
        assertEquals(expected, actual);
    }
}
