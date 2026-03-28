/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.MapperBuilder;

import java.util.function.Consumer;

/**
 * Used to store the shared {@link ObjectMapper} instance.
 */
public class ElideMapper {
    private ObjectMapper objectMapper;

    public ElideMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    public ElideMapper customizeObjectMapper(Consumer<MapperBuilder> customizer) {
        MapperBuilder builder = objectMapper.rebuild();
        customizer.accept(builder);
        this.objectMapper = builder.build();
        return this;
    }
}
