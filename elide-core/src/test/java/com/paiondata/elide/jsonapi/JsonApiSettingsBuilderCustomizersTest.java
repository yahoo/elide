/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.jsonapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.paiondata.elide.core.dictionary.EntityDictionary;

import org.junit.jupiter.api.Test;

/**
 * Test for JsonApiSettingsBuilderCustomizers.
 */
class JsonApiSettingsBuilderCustomizersTest {

    @Test
    void buildJsonApiSettingsBuilder() {
        JsonApiSettings jsonApiSettings = JsonApiSettingsBuilderCustomizers
                .buildJsonApiSettingsBuilder(EntityDictionary.builder().build(), builder -> {
                    builder.path("hello");
                }).build();
        assertEquals("hello", jsonApiSettings.getPath());
        assertNotEquals(0, jsonApiSettings.getJoinFilterDialects().size());
        assertNotEquals(0, jsonApiSettings.getSubqueryFilterDialects().size());
    }
}
