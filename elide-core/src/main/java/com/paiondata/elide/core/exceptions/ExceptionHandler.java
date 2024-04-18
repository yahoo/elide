/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.exceptions;

import com.paiondata.elide.ElideResponse;

/**
 * Exception handler.
 *
 * @param <C> the error context type
 */
@FunctionalInterface
public interface ExceptionHandler<C extends ErrorContext> {
    /**
     * Handle the exception.
     *
     * @param exception the exception to handle
     * @param errorContext the error context
     * @return the response
     */
    ElideResponse<?> handleException(Throwable exception, C errorContext);
}
