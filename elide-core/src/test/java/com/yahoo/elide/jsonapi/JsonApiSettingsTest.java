/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.jsonapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.jsonapi.JsonApiSettings.JsonApiSettingsBuilder;

import org.junit.jupiter.api.Test;


/**
 * Test for JsonApiSettings.
 */
class JsonApiSettingsTest {

    @Test
    void mutate() {
        JsonApiSettings jsonApiSettings = JsonApiSettings.builder().build();
        JsonApiSettings mutated = jsonApiSettings.mutate().links(links -> links.enabled(true)).build();
        assertNotEquals(jsonApiSettings.getLinks().isEnabled(), mutated.getLinks().isEnabled());
    }

    @Test
    void withDefaults() {
        EntityDictionary entityDictionary = EntityDictionary.builder().build();
        JsonApiSettings jsonApiSettings = JsonApiSettingsBuilder.withDefaults(entityDictionary).build();
        assertEquals(2, jsonApiSettings.getJoinFilterDialects().size());
        assertEquals(2, jsonApiSettings.getSubqueryFilterDialects().size());
    }

    @Test
    void updateStatus200() {
        assertEquals(200, JsonApiSettings.builder().updateStatus200().build().getUpdateStatusCode());
    }

    @Test
    void updateStatus204() {
        assertEquals(204, JsonApiSettings.builder().updateStatus204().build().getUpdateStatusCode());
    }
}
