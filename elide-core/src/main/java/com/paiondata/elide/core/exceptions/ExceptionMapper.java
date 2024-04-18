/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.exceptions;

import com.paiondata.elide.ElideErrorResponse;

import javax.annotation.Nullable;

/**
 * Maps an exception to an {@link ElideErrorResponse}.
 *
 * @param <E> exception type
 * @param <T> response body type
 */
@FunctionalInterface
public interface ExceptionMapper<E extends Throwable, T> {
    /**
     * Map the exception to an {@link ElideErrorResponse}.
     *
     * @param exception the exception to map.
     * @param errorContext the error context
     * @return the mapped ElideErrorResponse or null if you do not want to map this error
     */
    @Nullable
    ElideErrorResponse<? extends T> toErrorResponse(E exception, ErrorContext errorContext);
}
