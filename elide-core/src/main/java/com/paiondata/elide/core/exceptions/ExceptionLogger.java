/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.exceptions;

/**
 * Logger for Exceptions.
 *
 * @see ExceptionHandler
 */
@FunctionalInterface
public interface ExceptionLogger {
    /**
     * Log the exception.
     *
     * @param exception the exception to log
     */
    void log(Throwable exception);
}
