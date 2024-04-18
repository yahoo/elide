/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.exceptions;

import lombok.extern.slf4j.Slf4j;

/**
 * Occurs when many mappings to an entity are detected.
 */
@Slf4j
public class DuplicateMappingException extends RuntimeException {

    /**
     * Constructor.
     *
     * @param message the exception message
     */
    public DuplicateMappingException(String message) {
        this(message, null);
    }

    /* fast exception */
    public DuplicateMappingException(String message, Throwable cause) {
        super(message, cause, true, log.isTraceEnabled());
    }
}
