/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.graphql.federation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import graphql.schema.GraphQLAppliedDirective;

/**
 * Test for Federation Schema.
 *
 */
class FederationSchemaTest {

    @Test
    void applyFederation() {
        for (FederationVersion version : FederationVersion.values()) {
            assertNotNull(FederationSchema.getSchema(version));
        }
    }

    @Test
    void shareableNotPresentForFederation1() {
        FederationSchema schema = FederationSchema.builder().version(FederationVersion.FEDERATION_1_1).build();
        assertFalse(schema.shareable().isPresent());
    }

    @Test
    void shareablePresentForFederation2() {
        FederationSchema schema = FederationSchema.builder().version(FederationVersion.FEDERATION_2_0).build();
        assertTrue(schema.shareable().isPresent());
    }

    @Test
    void importWithNamespace() {
        GraphQLAppliedDirective directive = FederationSchema.builder().version(FederationVersion.FEDERATION_2_0).build()
                .key("id");
        assertTrue(directive.getName().startsWith(FederationDefinitions.namespace));
    }

    @Test
    void importWithoutNamespace() {
        GraphQLAppliedDirective directive = FederationSchema.builder().version(FederationVersion.FEDERATION_2_0)
                .imports("@key").build().key("id");
        assertEquals("key", directive.getName());
    }

    @Test
    void getSchema() {
        FederationSchema schema = FederationSchema.builder().version(FederationVersion.FEDERATION_2_0).build();
        assertNotNull(schema.getSchema());
    }

    @Test
    void getVersion() {
        FederationSchema schema = FederationSchema.builder().version(FederationVersion.FEDERATION_2_3).build();
        assertEquals(FederationVersion.FEDERATION_2_3, schema.getVersion());
    }
}
