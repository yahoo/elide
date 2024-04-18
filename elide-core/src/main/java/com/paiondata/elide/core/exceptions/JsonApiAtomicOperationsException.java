/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.exceptions;

import com.paiondata.elide.ElideErrorResponse;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Exception describing error caused from JSON API Atomic Extension request.
 */
public class JsonApiAtomicOperationsException extends HttpStatusException {
    private static final long serialVersionUID = 1L;
    private final ElideErrorResponse<JsonNode> response;

    public JsonApiAtomicOperationsException(int status, final JsonNode errorNode) {
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
