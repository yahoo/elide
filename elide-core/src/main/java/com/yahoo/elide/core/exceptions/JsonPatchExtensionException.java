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

import org.apache.commons.lang3.tuple.Pair;
import org.owasp.encoder.Encode;

/**
 * Exception describing error caused from Json Patch Extension request.
 */
public class JsonPatchExtensionException extends HttpStatusException {
    private final Pair<Integer, JsonNode> response;

    public JsonPatchExtensionException(int status, final JsonNode errorNode) {
        super(status, null);
        response = Pair.of(status, errorNode);
    }

    /**
     * @deprecated use {@link #getErrorResponse(boolean encodeResponse)}
     */
    @Deprecated
    public Pair<Integer, JsonNode> getResponse() {
        return response;
    }

    @Override
    public Pair<Integer, JsonNode> getErrorResponse() {
        return getErrorResponse(false);
    }

    @Override
    public Pair<Integer, JsonNode> getErrorResponse(boolean encodeResponse) {
        if (!encodeResponse) {
            return response;
        }

        return encodeResponse();
    }

    @Override
    public Pair<Integer, JsonNode> getVerboseErrorResponse() {
        return getVerboseErrorResponse(false);
    }

    @Override
    public Pair<Integer, JsonNode> getVerboseErrorResponse(boolean encodeResponse) {
        if (!encodeResponse) {
            return response;
        }

        return encodeResponse();
    }

    private Pair<Integer, JsonNode> encodeResponse() {
        // response is final, so construct a new response with encoded values
        ArrayNode encodedArray = JsonNodeFactory.instance.arrayNode();
        ArrayNode errors = (ArrayNode) response.getRight().get("errors");
        for (JsonNode node : errors) {
            ObjectNode objectNode = (ObjectNode) node;

            TextNode text = (TextNode) objectNode.get("detail");
            IntNode status = (IntNode) objectNode.get("status");

            ObjectNode encodedObjectNode = JsonNodeFactory.instance.objectNode();
            TextNode encodedTextNode = JsonNodeFactory.instance.textNode(Encode.forHtml(text.asText()));
            encodedObjectNode.set("detail", encodedTextNode);
            encodedObjectNode.set("status", status);
            encodedArray.add(encodedObjectNode);
        }
        return Pair.of(response.getLeft(), encodedArray);
    }
}
