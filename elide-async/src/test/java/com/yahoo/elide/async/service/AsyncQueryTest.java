/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import com.yahoo.elide.async.models.AsyncQuery;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AsyncQueryTest {

    private static Validator validator;

    @BeforeAll
    public void setupMocks() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    public void testMaxAsyncAfterSeconds() {
        AsyncQuery queryObj = new AsyncQuery();
        queryObj.setAsyncAfterSeconds(12);
        Set<ConstraintViolation<AsyncQuery>> constraintViolations = validator.validate(queryObj);
        assertEquals(1, constraintViolations.size());
        assertEquals("must be less than or equal to 10", constraintViolations.iterator().next().getMessage());
    }

    @Test
    public void testUUIDGeneration() {
        AsyncQuery queryObj = new AsyncQuery();
        assertNotNull(queryObj.getId());
    }
}
