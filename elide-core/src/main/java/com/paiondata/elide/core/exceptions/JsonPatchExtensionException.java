/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.exceptions;

import com.paiondata.elide.ElideErrorResponse;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Exception describing error caused from Json Patch Extension request.
 */
public class JsonPatchExtensionException extends HttpStatusException {
    private final ElideErrorResponse<JsonNode> response;

    public JsonPatchExtensionException(int status, final JsonNode errorNode) {
        super(status, "");
        response = ElideErrorResponse.status(status).body(errorNode);
    }

    @Override
    public ElideErrorResponse<JsonNode> getErrorResponse() {
        return response;
    }

    @Override
    public ElideErrorResponse<JsonNode> getVerboseErrorResponse() {
        return getErrorResponse();
    }
}
