/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.dynamicconfighelpers.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonValue;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Joins describe the SQL expression necessary to join two physical tables.
 * Joins can be used when defining dimension columns that reference other tables.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "name",
    "to",
    "type",
    "definition"
})
@Data
@EqualsAndHashCode()
@AllArgsConstructor
@NoArgsConstructor
public class Join {


    @JsonProperty("name")
    private String name;

    @JsonProperty("to")
    private String to;

    @JsonProperty("type")
    private Join.Type type;

    @JsonProperty("definition")
    private String definition;

    public enum Type {

        TO_ONE("toOne"),
        TO_MANY("toMany");
        private final String value;
        private final static Map<String, Join.Type> CONSTANTS = new HashMap<String, Join.Type>();

        static {
            for (Join.Type c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        private Type(String value) {
            this.value = value;
        }

        @JsonValue
        @Override
        public String toString() {
            return this.value;
        }

        @JsonCreator
        public static Join.Type fromValue(String value) {
            Join.Type constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }
    }
}
