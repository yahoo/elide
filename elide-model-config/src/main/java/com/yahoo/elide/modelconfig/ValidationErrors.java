/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.modelconfig;

import com.networknt.schema.Error;

import java.util.List;

public class ValidationErrors {
    private static final String NEWLINE = System.lineSeparator();

    public static String toString(List<Error> errors) {
        if (errors == null || errors.isEmpty()) {
            return null;
        }
        List<String> list = errors.stream().map(error -> error.toString()).toList();
        return NEWLINE + String.join(NEWLINE, list);
    }
}
