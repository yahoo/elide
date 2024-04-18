/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.jsonapi.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;

import java.io.IOException;
import java.util.Locale;

/**
 * The JSON Atomic Operation extension entity body.
 */
@JsonPropertyOrder({"op", "ref", "href", "data", "meta"})
@JsonInclude(Include.NON_NULL)
public class Operation {

    /**
     * Operation Code.
     */
    public enum OperationCode {
        /**
         * The ADD.
         */
        ADD(1, "add"),
        /**
         * The REMOVE.
         */
        REMOVE(2, "remove"),
        /**
         * The UPDATE.
         */
        UPDATE(3, "update");

        private final int id;
        private final String name;

        OperationCode(int id, String name) {
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
            return this.name;
        }

        public int getId() {
            return this.id;
        }

        @JsonCreator
        public static OperationCode fromName(String name) throws IOException {
            try {
                return name != null ? OperationCode.valueOf(name.toUpperCase(Locale.ENGLISH)) : null;
            } catch (RuntimeException e) {
                throw InvalidFormatException.from(null, e.getMessage(), name, OperationCode.class);
            }
        }
    }

    private final OperationCode operationCode;
    private final Ref ref;
    private final String href;
    private final JsonNode data;
    private final Meta meta;

    /**
     * Gets the operation code.
     *
     * @return operation of the patch
     */
    @JsonProperty("op")
    public OperationCode getOperationCode() {
        return this.operationCode;
    }

    /**
     * Gets the reference that identifies the target of the operation.
     *
     * @return The reference that identifies the target of the operation.
     */
    public Ref getRef() {
        return this.ref;
    }

    /**
     * Gets the URI-reference that identifies the target of the operation.
     *
     * @return The URI-reference that identifies the target of the operation.
     */
    public String getHref() {
        return this.href;
    }

    /**
     * Gets the data.
     *
     * @return The operation's primary data. This is either a single resource or
     *         array of resources.
     */
    public JsonNode getData() {
        return this.data;
    }

    /**
     * Gets the metadata.
     *
     * @return The metadata of the operation which contains non-standard
     *         meta-information about the operation.
     */
    public Meta getMeta() {
        return this.meta;
    }

    /**
     * Creates a new Atomic Operation entity body.
     *
     * @param operationCode The Operation Code which is either add, update or
     *                      remove.
     * @param ref           The reference that identifies the target of the operation.
     * @param href          The URI-reference that identifies the target of the operation.
     * @param data          The operation's primary data. This is either a single
     *                      resource or array of resources.
     * @param meta          The metadata of the operation which contains
     *                      non-standard meta-information about the operation.
     */
    @JsonCreator
    public Operation(@JsonProperty("op") OperationCode operationCode,
                 @JsonProperty("ref") Ref ref,
                 @JsonProperty("href") String href,
                 @JsonProperty("data") JsonNode data,
                 @JsonProperty("meta") Meta meta) {
        this.operationCode = operationCode;
        this.ref = ref;
        this.href = href;
        this.data = data;
        this.meta = meta;
    }
}
