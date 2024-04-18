/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.async;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

/**
 * Test for AsyncSettings.
 */
class AsyncSettingsTest {

    @Test
    void mutate() {
        AsyncSettings asyncSettings = AsyncSettings.builder().build();
        AsyncSettings mutated = asyncSettings.mutate().export(export -> export.enabled(true)).build();
        assertNotEquals(asyncSettings.getExport().isEnabled(), mutated.getExport().isEnabled());
    }
}
