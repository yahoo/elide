/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.modelconfig.model;

import static com.yahoo.elide.modelconfig.model.NamespaceConfig.DEFAULT;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * Joins describe the SQL expression necessary to join two physical tables.
 * Joins can be used when defining dimension columns that reference other tables.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "name",
    "namespace",
    "to",
    "type",
    "kind",
    "definition"
})
@Data
@EqualsAndHashCode()
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonDeserialize(builder = Join.JoinBuilder.class)
@JsonPOJOBuilder(withPrefix = "")
public class Join implements Named {
    private static final long serialVersionUID = -1416294756711914111L;

    @JsonProperty("name")
    private String name;

    @JsonProperty("namespace")
    @Builder.Default
    private String namespace = DEFAULT;

    @JsonProperty("to")
    private String to;

    @JsonProperty("type")
    private Join.Type type;

    @JsonProperty("kind")
    @Builder.Default
    private Join.Kind kind = Join.Kind.TOONE;

    @JsonProperty("definition")
    private String definition;

    /**
     * Returns the destination table of the join.
     * @return The global name of the destination join table.
     */
    public String getTo() {
        if (namespace == null || namespace.isEmpty() || namespace.equals(DEFAULT)) {
            return to;
        }

        return namespace + "_" + to;
    }

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
        INNER("inner"),
        FULL("full"),
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

    public static class JoinBuilder {
    }
}
