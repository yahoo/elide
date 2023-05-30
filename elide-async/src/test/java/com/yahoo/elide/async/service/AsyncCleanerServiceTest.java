/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import com.yahoo.elide.Elide;
import com.yahoo.elide.async.service.dao.AsyncAPIDAO;
import com.yahoo.elide.async.service.dao.DefaultAsyncAPIDAO;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.time.Duration;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AsyncCleanerServiceTest {

    private AsyncCleanerService service;

    @BeforeAll
    public void setupMocks() {
        Elide elide = mock(Elide.class);
        AsyncAPIDAO dao = mock(DefaultAsyncAPIDAO.class);
        AsyncCleanerService.init(elide, Duration.ofSeconds(5), Duration.ofDays(60), Duration.ofSeconds(300), dao);
        service = AsyncCleanerService.getInstance();
    }

    @Test
    public void testCleanerSet() {
        assertNotNull(service);
    }
}
