/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.exceptions;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;

/**
 * Test for Slf4jExceptionLogger.
 */
class Slf4jExceptionLoggerTest {
    private Logger logger = LoggerFactory.getLogger(Slf4jExceptionLogger.class);
    private Level level;

    @BeforeEach
    void setup() {
        ch.qos.logback.classic.Logger log = (ch.qos.logback.classic.Logger) logger;
        this.level = log.getLevel();
        log.setLevel(Level.DEBUG);
    }

    @AfterEach
    void teardown() {
        ch.qos.logback.classic.Logger log = (ch.qos.logback.classic.Logger) logger;
        log.setLevel(this.level);
    }

    @Test
    void forbiddenAccessException() {
        ForbiddenAccessException forbiddenAccessException = mock(ForbiddenAccessException.class);
        Slf4jExceptionLogger logger = new Slf4jExceptionLogger();
        logger.log(forbiddenAccessException);
        verify(forbiddenAccessException).getLoggedMessage();
    }

    @Test
    void httpStatusException() {
        HttpStatusException httpStatusException = mock(HttpStatusException.class);
        Slf4jExceptionLogger logger = new Slf4jExceptionLogger();
        logger.log(httpStatusException);
        verify(httpStatusException).getStatus();
    }

    @Test
    void illegalArgumentException() {
        IllegalArgumentException illegalArgumentException = mock(IllegalArgumentException.class);
        Slf4jExceptionLogger logger = new Slf4jExceptionLogger();
        logger.log(illegalArgumentException);
        verify(illegalArgumentException).getMessage();
    }
}
