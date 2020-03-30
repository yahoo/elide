/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.yahoo.elide.Elide;

public class AsyncCleanerServiceTest {

    private Elide elide;
    private AsyncQueryDAO dao;
    private AsyncCleanerService service;

    @BeforeEach
    public void setup() {
        elide = mock(Elide.class);
        dao = mock(DefaultAsyncQueryDAO.class);
        service = spy(new AsyncCleanerService(elide, 5, 5, dao));
    }

    @Test
    public void testCleanerSet() {
        assertNotNull(service.getCleaner());
    }
}
