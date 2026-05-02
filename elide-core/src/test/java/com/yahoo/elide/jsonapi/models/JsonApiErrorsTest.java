/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.jsonapi.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.Collections;

/**
 * Test for JsonApiErrors.
 */
class JsonApiErrorsTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void basicErrorObject() {
        JsonApiErrors errors = JsonApiErrors.builder()
                .error(error -> error.status("422").source(source -> source.pointer("/data/attributes/firstName"))
                        .title("Invalid Attribute").detail("First name must contain at least two characters."))
                .build();
        String actual = objectMapper.writeValueAsString(errors);
        String expected = """
                {"errors":[{"status":"422","source":{"pointer":"/data/attributes/firstName"},"title":"Invalid Attribute","detail":"First name must contain at least two characters."}]}""";
        assertEquals(expected, actual);
    }

    @Test
    void multipleErrors() {
        JsonApiErrors errors = JsonApiErrors.builder()
                .error(error -> error.status("403").source(source -> source.pointer("/data/attributes/secretPowers"))
                        .detail("Editing secret powers is not authorized on Sundays."))
                .error(error -> error.status("422").source(source -> source.pointer("/data/attributes/volume"))
                        .detail("Volume does not, in fact, go to 11."))
                .error(error -> error.status("500").source(source -> source.pointer("/data/attributes/reputation"))
                        .title("The backend responded with an error")
                        .detail("Reputation service not responding after three requests."))
                .build();
        String actual = objectMapper.writeValueAsString(errors);
        String expected = """
                {"errors":[{"status":"403","source":{"pointer":"/data/attributes/secretPowers"},"detail":"Editing secret powers is not authorized on Sundays."},{"status":"422","source":{"pointer":"/data/attributes/volume"},"detail":"Volume does not, in fact, go to 11."},{"status":"500","source":{"pointer":"/data/attributes/reputation"},"title":"The backend responded with an error","detail":"Reputation service not responding after three requests."}]}""";
        assertEquals(expected, actual);
    }

    @Test
    void meta() {
        JsonApiErrors errors = JsonApiErrors.builder()
                .error(error -> error.status("422").meta(meta -> meta.put("property", "property")))
                .build();
        String actual = objectMapper.writeValueAsString(errors);
        String expected = """
                {"errors":[{"status":"422","meta":{"property":"property"}}]}""";
        assertEquals(expected, actual);
    }

    @Test
    void emptyShouldThrow() {
        JsonApiErrors.JsonApiErrorsBuilder builder = JsonApiErrors.builder();
        assertThrows(IllegalArgumentException.class, () -> builder.build());
    }

    @Test
    void errorsConsumer() {
        JsonApiError error = JsonApiError.builder().detail("First name must contain at least two characters.").build();
        JsonApiErrors errorObjects = JsonApiErrors.builder().errors(errors -> errors.add(error)).build();
        String actual = objectMapper.writeValueAsString(errorObjects);
        String expected = """
                {"errors":[{"detail":"First name must contain at least two characters."}]}""";
        assertEquals(expected, actual);
    }

    @Test
    void errors() {
        JsonApiError error = JsonApiError.builder().detail("First name must contain at least two characters.").build();
        JsonApiErrors errorObjects = JsonApiErrors.builder().errors(Collections.singletonList(error)).build();
        String actual = objectMapper.writeValueAsString(errorObjects);
        String expected = """
                {"errors":[{"detail":"First name must contain at least two characters."}]}""";
        assertEquals(expected, actual);
    }
}
