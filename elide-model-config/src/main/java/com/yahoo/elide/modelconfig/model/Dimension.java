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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Dimensions represent labels for measures.
 * Dimensions are used to filter and group measures.
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
    "cardinality",
    "type",
    "grains",
    "tags",
    "arguments",
    "values",
    "tableSource"
})
@Data
@EqualsAndHashCode()
@AllArgsConstructor
@Builder
public class Dimension implements Named {

    @JsonProperty("name")
    private String name;

    @JsonProperty("friendlyName")
    private String friendlyName;

    @JsonProperty("description")
    private String description;

    @JsonProperty("category")
    private String category;

    @JsonProperty("hidden")
    private Boolean hidden;

    @JsonProperty("readAccess")
    private String readAccess;

    @JsonProperty("definition")
    private String definition;

    @JsonProperty("cardinality")
    private String cardinality;

    @JsonProperty("type")
    private Type type;

    @JsonProperty("grains")
    @Singular
    private List<Grain> grains;

    @JsonProperty("tags")
    @JsonDeserialize(as = LinkedHashSet.class)
    private Set<String> tags;

    @JsonProperty("arguments")
    @Singular
    private List<Argument> arguments;

    @JsonProperty("values")
    @JsonDeserialize(as = LinkedHashSet.class)
    private Set<String> values;

    @JsonProperty("tableSource")
    private String tableSource;

    public Dimension() {
        this.hidden = false;
        this.readAccess = "Prefab.Role.All";
        this.grains = new ArrayList<>();
        this.tags = new LinkedHashSet<>();
        this.values = new LinkedHashSet<>();
        this.arguments = new ArrayList<>();
    }

    /**
     * Returns description of the dimension.
     * If null, returns the name.
     * @return description
     */
    public String getDescription() {
        return (this.description == null ? getName() : this.description);
    }

    /**
     * Checks if this dimension has provided argument.
     * @param argName Name of the {@link Argument} to  check for.
     * @return true if this dimension has provided argument.
     */
    public boolean hasArgument(String argName) {
        return hasName(this.arguments, argName);
    }
}
