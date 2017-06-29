/*
 * Copyright 2016, Yahoo Inc.
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
import java.util.function.Supplier;

/**
 * Superclass for exceptions that return a Http error status.
 * Creates fast exceptions without stack traces since filter checks can throw many of these.
 */
@Slf4j
public abstract class HttpStatusException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    protected final int status;
    private final Supplier<String> verboseMessageSupplier;

    public HttpStatusException(int status) {
        this(status, null);
    }

    public HttpStatusException(int status, String message) {
        this(status, message, (Throwable) null, null);
    }

    @Deprecated
    public HttpStatusException(int status, String message, String verboseMessage) {
        this(status, message, verboseMessage, null);
    }

    @Deprecated
    public HttpStatusException(int status, String message, String verboseMessage, Throwable cause) {
        this(status, message, cause, () -> verboseMessage);
    }

    public HttpStatusException(int status, String message, Throwable cause, Supplier<String> verboseMessageSupplier) {
        super(message, cause, true, log.isTraceEnabled());
        this.status = status;
        this.verboseMessageSupplier = verboseMessageSupplier;
    }

    protected static String formatExceptionCause(Throwable e) {
        // if the throwable has a cause use that, otherwise the throwable is the cause
        Throwable error = e.getCause() == null ? e : e.getCause();
        return error == null ? null : error.getMessage() == null ? error.toString() : error.getMessage();
    }

    public Pair<Integer, JsonNode> getErrorResponse() {
        Map<String, List<String>> errors = Collections.singletonMap(
                "errors", Collections.singletonList(toString())
        );
        return buildResponse(errors);
    }

    public Pair<Integer, JsonNode> getVerboseErrorResponse() {
        Map<String, List<String>> errors = Collections.singletonMap(
                "errors", Collections.singletonList(getVerboseMessage())
        );
        return buildResponse(errors);
    }

    private Pair<Integer, JsonNode> buildResponse(Map<String, List<String>> errors) {
        JsonNode responseBody = OBJECT_MAPPER.convertValue(errors, JsonNode.class);
        return Pair.of(getStatus(), responseBody);
    }

    public String getVerboseMessage() {
        String verboseMessage = (verboseMessageSupplier == null) ? null : verboseMessageSupplier.get();
        return verboseMessage != null
                ? verboseMessage
                : toString();
    }

    public int getStatus() {
        return status;
    }

    @Override
    public String toString() {
        String message = getMessage();
        String className = getClass().getSimpleName();

        if (message == null) {
            message = className;
        } else {
            message = className + ": " + message;
        }

        return message;
    }
}
