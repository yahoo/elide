/*
 * Copyright 2018, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.exceptions;

import com.yahoo.elide.core.ErrorObjects;

import com.fasterxml.jackson.databind.JsonNode;

import org.apache.commons.lang3.tuple.Pair;

import java.util.Objects;

/**
 * Define your business exception extend this.
 */
public class CustomErrorException extends HttpStatusException {
    private static final long serialVersionUID = 1L;

    private final ErrorObjects errorObjects;

    /**
     * constructor.
     * @param status http status
     * @param message exception message
     * @param errorObjects custom error objects, not {@code null}
     */
    public CustomErrorException(int status, String message, ErrorObjects errorObjects) {
        this(status, message, null, errorObjects);
    }

    /**
     * constructor.
     * @param status http status
     * @param message exception message
     * @param cause the cause
     * @param errorObjects custom error objects, not {@code null}
     */
    public CustomErrorException(int status, String message, Throwable cause, ErrorObjects errorObjects) {
        super(status, message, cause, null);
        this.errorObjects = Objects.requireNonNull(errorObjects, "errorObjects must not be null");
    }

    @Override
    public Pair<Integer, JsonNode> getErrorResponse() {
        return buildCustomResponse();
    }

    @Override
    public Pair<Integer, JsonNode> getVerboseErrorResponse() {
        return buildCustomResponse();
    }

    private Pair<Integer, JsonNode> buildCustomResponse() {
        JsonNode responseBody = OBJECT_MAPPER.convertValue(errorObjects, JsonNode.class);
        return Pair.of(getStatus(), responseBody);
    }
}
