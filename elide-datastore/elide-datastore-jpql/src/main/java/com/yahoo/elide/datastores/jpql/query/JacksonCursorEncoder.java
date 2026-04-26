/*
 * Copyright 2024, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.jpql.query;

import com.yahoo.elide.core.exceptions.InvalidValueException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * {@link CursorEncoder} using Jackson.
 */
public class JacksonCursorEncoder implements CursorEncoder {
    private static class Holder {
        private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    }

    private final ObjectMapper objectMapper;

    public JacksonCursorEncoder() {
        this(Holder.OBJECT_MAPPER);
    }

    public JacksonCursorEncoder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String encode(Map<String, String> keys) {
        try {
            byte[] result = this.objectMapper.writeValueAsBytes(keys);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(result);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public Map<String, String> decode(String cursor) {
        if (cursor == null) {
            return Collections.emptyMap();
        }
        try {
            byte[] result = Base64.getUrlDecoder().decode(cursor);
            TypeReference<LinkedHashMap<String, String>> typeRef = new TypeReference<LinkedHashMap<String, String>>() {
            };
            return this.objectMapper.readValue(result, typeRef);
        } catch (IOException | IllegalArgumentException e) {
            throw new InvalidValueException("cursor " + cursor);
        }
    }
}
