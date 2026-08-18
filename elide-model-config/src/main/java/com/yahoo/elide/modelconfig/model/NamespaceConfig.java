/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.modelconfig.model;

import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Namespace Config JSON.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "name",
    "friendlyName",
    "readAccess",
    "description",
    "apiVersion"
})
@Data
@EqualsAndHashCode()
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NamespaceConfig implements Named {
    private static final long serialVersionUID = 279959092479649876L;

    public static String DEFAULT = "default";

    @JsonProperty("name")
    private String name;

    @JsonProperty("friendlyName")
    private String friendlyName;

    @Builder.Default
    @JsonProperty("readAccess")
    private String readAccess = "Prefab.Role.All";

    @JsonProperty("description")
    private String description;

    @Builder.Default
    @JsonProperty("apiVersion")
    private String apiVersion = EntityDictionary.NO_VERSION;

    /**
     * Returns description of the namespace object.
     * If null, returns the name.
     * @return description
     */
    public String getDescription() {
        return (this.description == null ? getName() : this.description);
    }
}
