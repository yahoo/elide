/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.exceptions;

import lombok.extern.slf4j.Slf4j;

/**
 * Slf4j Exception Logger.
 */
@Slf4j
public class Slf4jExceptionLogger implements ExceptionLogger {
    @Override
    public void log(Throwable exception) {
        if (log.isDebugEnabled()) {
            if (exception instanceof ForbiddenAccessException forbiddenAccessException) {
                log.debug("Caught {} {}", exception.getClass().getSimpleName(),
                        forbiddenAccessException.getLoggedMessage());
            } else if (exception instanceof HttpStatusException httpStatusException) {
                log.debug("Caught {} with status {}", exception.getClass().getSimpleName(),
                        httpStatusException.getStatus(), exception);
            } else {
                log.debug("Caught {}", exception.getClass().getSimpleName(), exception);
            }
        }
    }
}
