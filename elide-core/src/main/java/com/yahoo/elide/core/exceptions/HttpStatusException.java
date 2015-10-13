/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.exceptions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Superclass for exceptions that return a Http error status.
 * Creates fast exceptions without stack traces since filter checks can throw many of these.
 */
@Slf4j
public abstract class HttpStatusException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public abstract int getStatus();
    protected static ObjectMapper mapper = new ObjectMapper();


    public HttpStatusException() {
        this(null);
    }

    public HttpStatusException(String message) {
        this(message, null);
    }

    /* fast exceptions */
    public HttpStatusException(String message, Throwable cause) {
        super(message, cause, true, log.isTraceEnabled());
    }

    public Pair<Integer, JsonNode> getErrorResponse() {
        Map<String, List<String>> errors = Collections.singletonMap(
                "errors",
                Collections.singletonList(getMessage() == null ? toString() : getMessage()));
        JsonNode responseBody = mapper.convertValue(errors, JsonNode.class);
        return Pair.of(getStatus(), responseBody);
    }
}
