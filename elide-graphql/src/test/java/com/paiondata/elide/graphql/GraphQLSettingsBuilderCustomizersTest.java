/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.paiondata.elide.core.dictionary.EntityDictionary;

import org.junit.jupiter.api.Test;

/**
 * Test for GraphQLSettingsBuilderCustomizers.
 */
class GraphQLSettingsBuilderCustomizersTest {

    @Test
    void buildGraphQLSettingsBuilder() {
        GraphQLSettings graphqlSettings = GraphQLSettingsBuilderCustomizers
                .buildGraphQLSettingsBuilder(EntityDictionary.builder().build(), builder -> {
                    builder.path("hello");
                }).build();
        assertEquals("hello", graphqlSettings.getPath());
        assertNotNull(graphqlSettings.getFilterDialect());
    }

    @Test
    void buildGraphQLSettingsBuilderNull() {
        GraphQLSettings graphqlSettings = GraphQLSettingsBuilderCustomizers
                .buildGraphQLSettingsBuilder(EntityDictionary.builder().build(), null).path("test").build();
        assertEquals("test", graphqlSettings.getPath());
        assertNotNull(graphqlSettings.getFilterDialect());
    }
}
