/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.exceptions;

import com.paiondata.elide.core.request.Argument;
import com.paiondata.elide.core.request.Attribute;

/**
 * 400 Exception for invalid attribute parameters.
 */
public class InvalidParameterizedAttributeException extends HttpStatusException {
    public InvalidParameterizedAttributeException(Attribute attribute) {
        super(HttpStatus.SC_BAD_REQUEST, "No attribute found with matching parameters for attribute: " + attribute);
    }

    public InvalidParameterizedAttributeException(String attributeName, Argument argument) {
        super(HttpStatus.SC_BAD_REQUEST, String.format("Invalid argument : %s for attribute: %s",
                argument, attributeName));
    }
}
