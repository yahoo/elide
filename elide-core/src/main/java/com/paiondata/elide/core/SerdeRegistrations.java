/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core;

import com.paiondata.elide.core.utils.coerce.converters.Serde;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.io.IOException;

/**
 * Methods for operating on a {@link Serde}.
 */
public class SerdeRegistrations {
    /**
     * Register a {@link Serde} with a {@link ObjectMapper}.
     *
     * @param <S> The serialized type
     * @param <T> The deserialized type
     * @param objectMapper the object mapper to register with
     * @param type the deserialized type
     * @param serde the serde
     */
    public static <S, T> void register(ObjectMapper objectMapper, Class<T> type, Serde<S, T> serde) {
        register(objectMapper, type, serde, type.getSimpleName());
    }

    /**
     * Register a {@link Serde} with a {@link ObjectMapper}.
     *
     * @param <S> The serialized type
     * @param <T> The deserialized type
     * @param objectMapper the object mapper to register with
     * @param type the deserialized type
     * @param serde the serde
     * @param name the name to register with
     */
    public static <S, T> void register(ObjectMapper objectMapper, Class<T> type, Serde<S, T> serde,
            String name) {
        objectMapper.registerModule(new SimpleModule(name).addSerializer(type, new JsonSerializer<T>() {
            @Override
            public void serialize(T value, JsonGenerator jsonGenerator, SerializerProvider serializerProvider)
                    throws IOException, JsonProcessingException {
                jsonGenerator.writeObject(serde.serialize(value));
            }
        }));
    }
}
