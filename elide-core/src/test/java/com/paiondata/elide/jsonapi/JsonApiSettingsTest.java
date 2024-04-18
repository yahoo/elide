/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.jsonapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.filter.dialect.jsonapi.DefaultFilterDialect;
import com.paiondata.elide.core.filter.dialect.jsonapi.JoinFilterDialect;
import com.paiondata.elide.core.filter.dialect.jsonapi.SubqueryFilterDialect;
import com.paiondata.elide.jsonapi.JsonApiSettings.JsonApiSettingsBuilder;

import org.junit.jupiter.api.Test;

import java.util.List;

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

    @Test
    void joinFilterDialectsConsumer() {
        JoinFilterDialect joinFilterDialect = new DefaultFilterDialect(EntityDictionary.builder().build());
        JsonApiSettings jsonApiSettings = JsonApiSettings.builder().joinFilterDialect(joinFilterDialect)
                .joinFilterDialects(joinFilterDialects -> {
                    joinFilterDialects.add(joinFilterDialect);
                }).build();
        assertEquals(2, jsonApiSettings.getJoinFilterDialects().size());
        assertSame(joinFilterDialect, jsonApiSettings.getJoinFilterDialects().get(0));
        assertSame(joinFilterDialect, jsonApiSettings.getJoinFilterDialects().get(1));
    }

    @Test
    void subqueryFilterDialectsConsumer() {
        SubqueryFilterDialect subqueryFilterDialect = new DefaultFilterDialect(EntityDictionary.builder().build());
        JsonApiSettings jsonApiSettings = JsonApiSettings.builder().subqueryFilterDialect(subqueryFilterDialect)
                .subqueryFilterDialects(subqueryFilterDialects -> {
                    subqueryFilterDialects.add(subqueryFilterDialect);
                }).build();
        assertEquals(2, jsonApiSettings.getSubqueryFilterDialects().size());
        assertSame(subqueryFilterDialect, jsonApiSettings.getSubqueryFilterDialects().get(0));
        assertSame(subqueryFilterDialect, jsonApiSettings.getSubqueryFilterDialects().get(1));
    }

    @Test
    void joinFilterDialectsList() {
        JoinFilterDialect joinFilterDialect = new DefaultFilterDialect(EntityDictionary.builder().build());
        JsonApiSettings jsonApiSettings = JsonApiSettings.builder().joinFilterDialects(List.of(joinFilterDialect))
                .build();
        assertEquals(1, jsonApiSettings.getJoinFilterDialects().size());
        assertSame(joinFilterDialect, jsonApiSettings.getJoinFilterDialects().get(0));
    }

    @Test
    void subqueryFilterDialectsList() {
        SubqueryFilterDialect subqueryFilterDialect = new DefaultFilterDialect(EntityDictionary.builder().build());
        JsonApiSettings jsonApiSettings = JsonApiSettings.builder()
                .subqueryFilterDialects(List.of(subqueryFilterDialect)).build();
        assertEquals(1, jsonApiSettings.getSubqueryFilterDialects().size());
        assertSame(subqueryFilterDialect, jsonApiSettings.getSubqueryFilterDialects().get(0));
    }
}
