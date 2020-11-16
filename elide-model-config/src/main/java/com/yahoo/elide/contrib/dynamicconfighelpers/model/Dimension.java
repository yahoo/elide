/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.dynamicconfighelpers.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Dimensions represent labels for measures.
 * Dimensions are used to filter and group measures.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "name",
    "description",
    "category",
    "hidden",
    "readAccess",
    "definition",
    "cardinality",
    "type",
    "grain",
    "tags",
    "values",
    "tableSource"
})
@Data
@EqualsAndHashCode()
@AllArgsConstructor
@NoArgsConstructor
public class Dimension implements Named {

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("category")
    private String category;

    @JsonProperty("hidden")
    private Boolean hidden = false;

    @JsonProperty("readAccess")
    private String readAccess = "Prefab.Role.All";

    @JsonProperty("definition")
    private String definition = "";

    @JsonProperty("cardinality")
    private String cardinality;

    @JsonProperty("type")
    private Type type = Type.TEXT;

    @JsonProperty("grain")
    private Grain grain = new Grain();

    @JsonProperty("tags")
    @JsonDeserialize(as = LinkedHashSet.class)
    private Set<String> tags = new LinkedHashSet<String>();

    @JsonProperty("values")
    @JsonDeserialize(as = LinkedHashSet.class)
    private Set<String> values = new LinkedHashSet<String>();

    @JsonProperty("tableSource")
    private String tableSource = "";

    @JsonIgnore
    private boolean override = false;

    /**
     * Returns description of the dimension.
     * If null, returns the name.
     * @return description
     */
    public String getDescription() {
        return (this.description == null ? getName() : this.description);
    }
}
