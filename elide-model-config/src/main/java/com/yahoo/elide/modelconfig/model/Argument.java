/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.modelconfig.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Argument Model.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "name",
    "description",
    "type",
    "values",
    "tableSource",
    "default"
})
@Data
@EqualsAndHashCode()
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Argument implements Named {
    private static final long serialVersionUID = -6628282044575311784L;

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("type")
    private String type;

    @JsonProperty("values")
    @JsonDeserialize(as = LinkedHashSet.class)
    private Set<String> values = new LinkedHashSet<>();

    @JsonProperty("tableSource")
    private TableSource tableSource;

    @JsonProperty("default")
    private Object defaultValue;

    /**
     * Returns description of the argument.
     * If null, returns the name
     * @return description
     */
    public String getDescription() {
        return (this.description == null ? getName() : this.description);
    }
}
