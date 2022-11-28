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
 * Elide Security POJO.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "roles",
    "rules"
})
@Data
@EqualsAndHashCode()
@AllArgsConstructor
@NoArgsConstructor
public class ElideSecurityConfig {

    @JsonProperty("roles")
    @JsonDeserialize(as = LinkedHashSet.class)
    private Set<String> roles = new LinkedHashSet<>();

    @JsonProperty("rules")
    @JsonDeserialize(as = LinkedHashSet.class)
    private Set<Rule> rules = new LinkedHashSet<>();

    public boolean hasCheckDefined(String role) {

        return roles
                   .stream()
                   .anyMatch(role::equals);
    }
}
