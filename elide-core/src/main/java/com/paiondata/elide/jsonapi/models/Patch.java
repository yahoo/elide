/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.jsonapi.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;

import java.io.IOException;
import java.util.Locale;

/**
 * POJO which represents a JSON API patch extension entity body.
 */
@JsonPropertyOrder({"op", "path", "value"})
public class Patch {

    /**
     * Patch operation.
     */
    public enum Operation {
        /**
         * The ADD.
         */
        ADD(1, "add"),
        /**
         * The REMOVE.
         */
        REMOVE(2, "remove"),
        /**
         * The REPLACE.
         */
        REPLACE(3, "replace");

        private final int id;
        private final String name;

        Operation(int id, String name) {
            this.id = id;
            this.name = name;
        }

        /**
         * Gets name.
         *
         * @return the name
         */
        @JsonValue
        public String getName() {
            return name;
        }

        public int getId() {
            return this.id;
        }

        @JsonCreator
        public static Operation fromName(String name) throws IOException {
            try {
                return name != null ? Operation.valueOf(name.toUpperCase(Locale.ENGLISH)) : null;
            } catch (RuntimeException e) {
                throw InvalidFormatException.from(null, e.getMessage(), name, Operation.class);
            }
        }
    }

    private final Operation operation;
    private final String path;
    private final JsonNode value;

    /**
     * Gets operation.
     *
     * @return operation of the patch
     */
    @JsonProperty("op")
    public Operation getOperation() {
        return operation;
    }

    /**
     * Gets path.
     *
     * @return URI of the patch
     */
    public String getPath() {
        return path;
    }

    /**
     * Gets value.
     *
     * @return The value of the patch which is context sensitive and will represent another JSON API entity.
     */
    public JsonNode getValue() {
        return value;
    }

    /**
     * Creates a new patch entity body POJO.
     *
     * @param operation the operation
     * @param path the path
     * @param value the value
     */
    @JsonCreator
    public Patch(@JsonProperty("op") Operation operation,
                 @JsonProperty("path") String path,
                 @JsonProperty("value") JsonNode value) {
        this.operation = operation;
        this.path = path;
        this.value = value;
    }
}
