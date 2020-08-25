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
 * Grain can have SQL expressions that can substitute column
 * with the dimension definition expression.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "type",
    "sql"
})
@Data
@EqualsAndHashCode()
@AllArgsConstructor
@NoArgsConstructor
public class Grain {


    @JsonProperty("type")
    private Grain.GrainType type;

    @JsonProperty("sql")
    private String sql;

    public enum GrainType {

        SIMPLEDATE("SIMPLEDATE"),
        DATETIME("DATETIME"),
        YEARMONTH("YEARMONTH"),
        YEAR("YEAR"),
        MONTHYEAR("MONTHYEAR"),
        WEEKDATE("WEEKDATE");

        private final String value;
        private final static Map<String, Grain.GrainType> CONSTANTS = new HashMap<String, Grain.GrainType>();

        static {
            for (Grain.GrainType c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        private GrainType(String value) {
            this.value = value;
        }

        @JsonValue
        @Override
        public String toString() {
            return this.value;
        }

        @JsonCreator
        public static Grain.GrainType fromValue(String value) {
            Grain.GrainType constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }
    }
}
