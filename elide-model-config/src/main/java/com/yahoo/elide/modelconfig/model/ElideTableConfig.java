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
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Elide Table POJO.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "tables"
})
@Data
@EqualsAndHashCode()
@AllArgsConstructor
@NoArgsConstructor
public class ElideTableConfig {

    @JsonProperty("tables")
    @JsonDeserialize(as = LinkedHashSet.class)
    private Set<Table> tables = new LinkedHashSet<>();

    /**
     * Checks if a dynamic model exists with the given name.
     * @param name Model Name
     * @return true if a dynamic model exists with the given name.
     */
    public boolean hasTable(String name) {
        return tables
                   .stream()
                   .map(Named::getGlobalName)
                   .anyMatch(name::equals);
    }

    /**
     * Provides the dynamic model with the given name.
     * @param name Model Name
     * @return dynamic model with the given name.
     */
    public Table getTable(String name) {
        return tables
                   .stream()
                   .filter(t -> t.getGlobalName().equals(name))
                   .findFirst()
                   .orElse(null);
    }
}
