/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.exceptions;

public class UnableToAddSerdeException extends RuntimeException {
    public UnableToAddSerdeException(String message) {
        super(message);
    }

    public UnableToAddSerdeException(String message, Throwable cause) {
        super(message, cause);
    }
}
