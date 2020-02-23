/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.exceptions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import com.yahoo.elide.core.ErrorObjects;
import org.apache.commons.lang3.tuple.Pair;
import org.owasp.encoder.Encode;

/**
 * Exception describing error caused from Json Patch Extension request.
 */
public class JsonPatchExtensionException extends CustomErrorException {
    private final Pair<Integer, JsonNode> response;

    public JsonPatchExtensionException(int status, final JsonNode errorNode) {
        super(status, "", encodeResponse(errorNode));
        response = Pair.of(status, errorNode);
    }

    private static ErrorObjects encodeResponse(JsonNode response) {
        ErrorObjects.ErrorObjectsBuilder errorObjectsBuilder = ErrorObjects.builder();
        // response is final, so construct a new response with encoded values
        ArrayNode errors = (ArrayNode) response.get("errors");
        for (JsonNode node : errors) {

            ObjectNode objectNode = (ObjectNode) node;
            errorObjectsBuilder.addError();
            errorObjectsBuilder = errorObjectsBuilder.withDetail(Encode.forHtml(objectNode.get("detail").asText()));

            if (objectNode.get("status") != null) {
                errorObjectsBuilder = errorObjectsBuilder.withStatus(objectNode.get("status").asText());
            }
        }
        return errorObjectsBuilder.build();
    }
}
