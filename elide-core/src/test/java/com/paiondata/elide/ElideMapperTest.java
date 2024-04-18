/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

/**
 * Test for ElideMapper.
 */
class ElideMapperTest {

    @Test
    void constructor() {
        ObjectMapper objectMapper = new ObjectMapper();
        ElideMapper mapper = new ElideMapper(objectMapper);
        assertEquals(objectMapper, mapper.getObjectMapper());
    }
}
