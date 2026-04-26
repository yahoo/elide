/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.modelconfig;

import com.networknt.schema.ValidationMessage;

import java.util.List;
import java.util.Set;

public class ValidationMessages {
    private static final String NEWLINE = System.lineSeparator();

    public static String toString(Set<ValidationMessage> results) {
        if (results == null || results.isEmpty()) {
            return null;
        }
        List<String> list = results.stream().map(message -> message.getMessage()).toList();
        return NEWLINE + String.join(NEWLINE, list);
    }
}
