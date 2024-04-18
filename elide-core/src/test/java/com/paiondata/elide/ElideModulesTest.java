/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Test for ElideModules.
 */
class ElideModulesTest {

    @Test
    void isGraphQLPresent() {
        assertFalse(ElideModules.isGraphQLPresent());
    }

    @Test
    void isAsyncPresent() {
        assertFalse(ElideModules.isAsyncPresent());
    }

    @Test
    void isJsonApiPresent() {
        assertTrue(ElideModules.isJsonApiPresent());
    }
}
