/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import java.util.Map;

/**
 * Test for ElideError.
 */
class ElideErrorTest {

    @Test
    void attributesCustomizer() {
        ElideError error = ElideError.builder().attributes(attributes -> attributes.put("key", "value")).build();
        assertEquals("value", error.getAttributes().get("key"));
    }

    @Test
    void attributes() {
        ElideError error = ElideError.builder().attributes(Map.of("key", "value")).build();
        assertEquals("value", error.getAttributes().get("key"));
    }
}
