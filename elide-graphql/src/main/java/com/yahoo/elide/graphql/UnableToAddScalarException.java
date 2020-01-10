/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql;

public class UnableToAddScalarException extends RuntimeException {
    public UnableToAddScalarException(String message) {
        super(message);
    }
}
