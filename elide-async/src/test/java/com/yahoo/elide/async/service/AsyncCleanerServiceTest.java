/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.mockito.Mockito;

import com.yahoo.elide.Elide;
import com.yahoo.elide.async.service.AsyncCleanerService;
import com.yahoo.elide.async.service.AsyncQueryDAO;
import com.yahoo.elide.async.service.DefaultAsyncQueryDAO;

public class AsyncCleanerServiceTest {

    @Test
    public void testCleanerSet() {
        Elide elide = Mockito.mock(Elide.class);
        AsyncQueryDAO dao = Mockito.mock(DefaultAsyncQueryDAO.class);
        AsyncCleanerService service = Mockito.spy(new AsyncCleanerService(elide, 5, 5, dao));
        assertNotNull(service.getCleaner());
    }
}
