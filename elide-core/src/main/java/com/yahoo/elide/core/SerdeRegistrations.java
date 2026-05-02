/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

import com.yahoo.elide.core.utils.coerce.converters.Serde;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.cfg.MapperBuilder;
import tools.jackson.databind.module.SimpleModule;

/**
 * Methods for operating on a {@link Serde}.
 */
public class SerdeRegistrations {
    /**
     * Register a {@link Serde} with a {@link ObjectMapper}.
     *
     * @param <S> The serialized type
     * @param <T> The deserialized type
     * @param builder the object mapper to register with
     * @param type the deserialized type
     * @param serde the serde
     */
    public static <S, T> void register(MapperBuilder builder, Class<T> type, Serde<S, T> serde) {
        register(builder, type, serde, type.getSimpleName());
    }

    /**
     * Register a {@link Serde} with a {@link ObjectMapper}.
     *
     * @param <S> The serialized type
     * @param <T> The deserialized type
     * @param builder the object mapper to register with
     * @param type the deserialized type
     * @param serde the serde
     * @param name the name to register with
     */
    public static <S, T> void register(MapperBuilder builder, Class<T> type, Serde<S, T> serde,
            String name) {
        builder.addModule(new SimpleModule(name).addSerializer(type, new ValueSerializer<T>() {
            @Override
            public void serialize(T value, JsonGenerator jsonGenerator, SerializationContext serializationContext) {
                jsonGenerator.writePOJO(serde.serialize(value));
            }
        }));
    }
}
