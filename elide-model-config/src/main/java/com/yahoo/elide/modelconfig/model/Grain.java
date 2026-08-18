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
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

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
@Builder
public class Grain implements Serializable {
    private static final long serialVersionUID = -6253818551445562327L;

    @JsonProperty("type")
    private Grain.GrainType type;

    @JsonProperty("sql")
    private String sql;

    public enum GrainType {

        DAY("DAY"),
        HOUR("HOUR"),
        ISOWEEK("ISOWEEK"),
        MINUTE("MINUTE"),
        MONTH("MONTH"),
        QUARTER("QUARTER"),
        SECOND("SECOND"),
        WEEK("WEEK"),
        YEAR("YEAR");

        private final String value;

        private GrainType(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }
    }
}
