/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.dynamicconfig.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;


@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "name",
    "description",
    "category",
    "hidden",
    "readAccess",
    "definition",
    "type"
})
@Data
@EqualsAndHashCode()
@AllArgsConstructor
@NoArgsConstructor
public class Measure {

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("category")
    private String category;

    @JsonProperty("hidden")
    private Boolean hidden = false;

    @JsonProperty("readAccess")
    private String readAccess = "Allow All";

    @JsonProperty("definition")
    private String definition;

    @JsonProperty("type")
    private Type type = Type.INTEGER;

    //  default behaviour, in case description is null
    public String getDescription() {
        return (this.description == null ? getName() : this.description);
    }
}
