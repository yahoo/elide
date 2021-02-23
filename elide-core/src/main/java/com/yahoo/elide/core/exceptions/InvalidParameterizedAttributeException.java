/*
 * Copyright 2021, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.exceptions;

import com.yahoo.elide.core.request.Argument;
import com.yahoo.elide.core.request.Attribute;

/**
 * 400 Exception for invalid attribute parameters.
 */
public class InvalidParameterizedAttributeException extends HttpStatusException {
    public InvalidParameterizedAttributeException(Attribute attribute) {
        super(HttpStatus.SC_BAD_REQUEST, "No attribute found with matching parameters for attribute: "
                + attribute.toString());
    }

    public InvalidParameterizedAttributeException(String attributeName, Argument argument) {
        super(HttpStatus.SC_BAD_REQUEST, String.format("Invalid argument : %s for attribute: %s",
                argument, attributeName));
    }
}
