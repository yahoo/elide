/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.exceptions;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Exception describing error caused from Json Patch Extension request.
 */
public class JsonPatchExtensionException extends HttpStatusException {
    private final Pair<Integer, JsonNode> response;

    public JsonPatchExtensionException(int status, final JsonNode errorNode) {
        super(status);
        response = Pair.of(status, errorNode);
    }

    public JsonPatchExtensionException(final Pair<Integer, JsonNode> response) {
        super(response.getLeft());
        this.response = response;
    }

    public Pair<Integer, JsonNode> getResponse() {
        return response;
    }
}
