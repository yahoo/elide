/*
 * Copyright 2020, Yahoo Inc.
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
import lombok.Singular;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Measures represent metrics that can be aggregated at query time.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "name",
    "friendlyName",
    "description",
    "category",
    "hidden",
    "readAccess",
    "definition",
    "queryPlanResolver",
    "type",
    "tags",
    "arguments",
})
@Data
@EqualsAndHashCode()
@AllArgsConstructor
@Builder
public class Measure implements Named {

    @JsonProperty("name")
    private final String name;

    @JsonProperty("friendlyName")
    private final String friendlyName;

    @JsonProperty("description")
    private final String description;

    @JsonProperty("category")
    private final String category;

    @JsonProperty("hidden")
    private final Boolean hidden;

    @JsonProperty("readAccess")
    private final String readAccess;

    @JsonProperty("definition")
    private final String definition;

    @JsonProperty("type")
    private final Type type;

    @JsonProperty("queryPlanResolver")
    private final String queryPlanResolver;

    @JsonProperty("tags")
    @JsonDeserialize(as = LinkedHashSet.class)
    private final Set<String> tags;

    @JsonProperty("arguments")
    @Singular
    private final List<Argument> arguments;

    /**
     * Returns description of the measure.
     * If null, returns the name.
     * @return description
     */
    public String getDescription() {
        return (this.description == null ? getName() : this.description);
    }

    /**
     * Checks if this measure has provided argument.
     * @param argName Name of the {@link Argument} to  check for.
     * @return true if this measure has provided argument.
     */
    public boolean hasArgument(String argName) {
        return hasName(this.arguments, argName);
    }
}
