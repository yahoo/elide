/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.exceptions;

/**
 * Invalid predicate exception.
 */
@Deprecated
public class InvalidPredicateException extends BadRequestException {
    public InvalidPredicateException(String message) {
        super(message);
    }
}
