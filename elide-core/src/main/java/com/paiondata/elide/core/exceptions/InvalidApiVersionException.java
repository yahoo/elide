/*
 * Copyright 2023, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.exceptions;

/**
 * Exception representing invalid api version specified.
 */
public class InvalidApiVersionException extends InvalidOperationException {
    private static final long serialVersionUID = 1L;

    public InvalidApiVersionException(String body) {
        super(body);
    }
}
