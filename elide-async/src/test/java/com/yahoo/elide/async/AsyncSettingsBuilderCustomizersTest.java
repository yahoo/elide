/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.async;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests for AsyncSettingsBuilderCustomizers.
 */
class AsyncSettingsBuilderCustomizersTest {

    @Test
    void buildNull() {
        AsyncSettings asyncSettings = AsyncSettingsBuilderCustomizers.buildAsyncSettingsBuilder(null).enabled(true)
                .build();
        assertTrue(asyncSettings.isEnabled());
    }

    @Test
    void build() {
        AsyncSettings asyncSettings = AsyncSettingsBuilderCustomizers
                .buildAsyncSettingsBuilder(builder -> builder.enabled(true)).build();
        assertTrue(asyncSettings.isEnabled());
    }
}
