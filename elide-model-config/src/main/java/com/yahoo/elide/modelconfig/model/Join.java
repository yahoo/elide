/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.modelconfig.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Joins describe the SQL expression necessary to join two physical tables.
 * Joins can be used when defining dimension columns that reference other tables.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "name",
    "to",
    "type",
    "kind",
    "definition"
})
@Data
@EqualsAndHashCode()
@AllArgsConstructor
@NoArgsConstructor
public class Join implements Named {

    @JsonProperty("name")
    private String name;

    @JsonProperty("to")
    private String to;

    @JsonProperty("type")
    private Join.Type type;

    @JsonProperty("kind")
    private Join.Kind kind;

    @JsonProperty("definition")
    private String definition;

    public enum Kind {

        TOONE("toOne"),
        TOMANY("toMany");

        private final String value;

        private Kind(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }
    }

    public enum Type {

        LEFT("left"),
        RIGHT("right"),
        INNER("inner"),
        OUTER("outer"),
        CROSS("cross");

        private final String value;

        private Type(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }
    }
}
