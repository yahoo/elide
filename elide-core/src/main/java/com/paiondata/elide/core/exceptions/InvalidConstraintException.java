/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.exceptions;
import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.type.ClassType;

/**
 * Invalid constraint exception. Message is exactly what is provided in the constructor.
 *
 * {@link HttpStatus#SC_BAD_REQUEST invalid}
 */
public class InvalidConstraintException extends HttpStatusException {
    private static final long serialVersionUID = 1L;

    public InvalidConstraintException(String message) {
        super(HttpStatus.SC_BAD_REQUEST, message);
    }

    @Override
    public String toString() {
        String message = getMessage();

        if (message == null) {
            return EntityDictionary.getSimpleName(ClassType.of(getClass()));
        }

        return message;
    }
}
