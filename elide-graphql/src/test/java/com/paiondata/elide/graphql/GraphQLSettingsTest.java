/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.graphql;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.graphql.GraphQLSettings.GraphQLSettingsBuilder;

import org.junit.jupiter.api.Test;

/**
 * Test for GraphQLSettings.
 */
class GraphQLSettingsTest {

    @Test
    void mutate() {
        GraphQLSettings graphqlSettings = GraphQLSettings.builder().build();
        GraphQLSettings mutated = graphqlSettings.mutate().federation(federation -> federation.enabled(true)).build();
        assertNotEquals(graphqlSettings.getFederation().isEnabled(), mutated.getFederation().isEnabled());
    }

    @Test
    void withDefaults() {
        EntityDictionary entityDictionary = EntityDictionary.builder().build();
        GraphQLSettings graphqlSettings = GraphQLSettingsBuilder.withDefaults(entityDictionary).build();
        assertNotNull(graphqlSettings.getFilterDialect());
    }

    @Test
    void enabledTrue() {
        GraphQLSettings graphqlSettings = GraphQLSettings.builder().enabled(true).build();
        assertTrue(graphqlSettings.isEnabled());
    }

    @Test
    void enabledFalse() {
        GraphQLSettings graphqlSettings = GraphQLSettings.builder().enabled(false).build();
        assertFalse(graphqlSettings.isEnabled());
    }
}
