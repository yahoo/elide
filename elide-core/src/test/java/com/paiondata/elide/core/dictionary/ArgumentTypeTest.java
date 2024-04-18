/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.dictionary;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.paiondata.elide.core.type.ClassType;

import org.junit.jupiter.api.Test;

/**
 * Test for ArgumentType.
 */
class ArgumentTypeTest {

    @Test
    void builder() {
        ArgumentType argumentType = ArgumentType.builder().name("name").defaultValue("default")
                .type(ClassType.of(ArgumentTypeTest.class)).build();
        assertEquals("default", argumentType.getDefaultValue());
        assertEquals("name", argumentType.getName());
        assertEquals(ClassType.of(ArgumentTypeTest.class), argumentType.getType());
        assertEquals(argumentType.hashCode(), argumentType.hashCode());
    }

    @Test
    void equals() {
        assertEquals(ArgumentType.builder().build(), ArgumentType.builder().build());
    }
}
