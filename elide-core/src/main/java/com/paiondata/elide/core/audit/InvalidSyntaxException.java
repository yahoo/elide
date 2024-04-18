/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.audit;

/**
 * Thrown if audit has been configured incorrectly by the programmer.
 */
public class InvalidSyntaxException extends RuntimeException {
    public InvalidSyntaxException(String reason) {
        super(reason);
    }
    public InvalidSyntaxException(Exception cause) {
        super(cause);
    }
    public InvalidSyntaxException(String message, Throwable cause) {
        super(message, cause);
    }
}
