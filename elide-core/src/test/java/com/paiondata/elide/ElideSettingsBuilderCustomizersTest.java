/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Test for ElideSettingsBuilderCustomizers.
 */
class ElideSettingsBuilderCustomizersTest {

    @Test
    void build() {
        ElideSettings elideSettings = ElideSettingsBuilderCustomizers
                .buildElideSettingsBuilder(builder -> builder.baseUrl("baseUrl")).build();
        assertEquals("baseUrl", elideSettings.getBaseUrl());
    }

    @Test
    void buildNull() {
        ElideSettings elideSettings = ElideSettingsBuilderCustomizers
                .buildElideSettingsBuilder(null).baseUrl("baseUrl").build();
        assertEquals("baseUrl", elideSettings.getBaseUrl());
    }
}
