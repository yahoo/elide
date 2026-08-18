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
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Elide Namespace POJO.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "namespaces"
})
@Data
@EqualsAndHashCode()
@AllArgsConstructor
@NoArgsConstructor
public class ElideNamespaceConfig {

    @JsonProperty("namespaces")
    @JsonDeserialize(as = LinkedHashSet.class)
    private Set<NamespaceConfig> namespaceconfigs = new LinkedHashSet<>();

    /**
     * Checks if a namespace exists with the given name.
     * @param name namespace Name
     * @return true if a dynamic namespace exists with the given name.
     */
    public boolean hasNamespace(String name, String version) {
        String nameLower = name.toLowerCase(Locale.ENGLISH);
        if (nameLower.equals(DEFAULT)) {
            return true;
        }

        return namespaceconfigs
                .stream()
                .filter(namespace -> namespace.getApiVersion().equals(version))
                .map(Named::getName)
                .anyMatch(name::equals);
    }
}
