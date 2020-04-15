/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import com.yahoo.elide.Elide;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AsyncCleanerServiceTest {

    private Elide elide;
    private AsyncQueryDAO dao;
    private AsyncCleanerService service;

    @BeforeAll
    public void setup() {
        elide = mock(Elide.class);
        dao = mock(DefaultAsyncQueryDAO.class);
        AsyncCleanerService.init(elide, 5, 60, dao);
        service = AsyncCleanerService.getInstance();
    }

    @Test
    public void testCleanerSet() {
        assertNotNull(service.getCleaner());
    }
}
