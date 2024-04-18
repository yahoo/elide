/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.request.route;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.paiondata.elide.core.dictionary.EntityDictionary;

import org.junit.jupiter.api.Test;

/**
 * Test for BasicApiVersionValidator.
 */
class BasicApiVersionValidatorTest {

    @Test
    void noVersionIsValid() {
        BasicApiVersionValidator validator = new BasicApiVersionValidator();
        assertTrue(validator.isValidApiVersion(EntityDictionary.NO_VERSION));
    }

    @Test
    void nullVersionIsNotValid() {
        BasicApiVersionValidator validator = new BasicApiVersionValidator();
        assertFalse(validator.isValidApiVersion(null));
    }

    @Test
    void digitVersionIsValid() {
        BasicApiVersionValidator validator = new BasicApiVersionValidator();
        assertTrue(validator.isValidApiVersion("3"));
    }

    @Test
    void letterVersionIsNotValid() {
        BasicApiVersionValidator validator = new BasicApiVersionValidator();
        assertFalse(validator.isValidApiVersion("a"));
    }
}
