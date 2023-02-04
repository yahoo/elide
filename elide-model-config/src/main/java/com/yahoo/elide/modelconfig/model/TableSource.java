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
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * TableSource is a reference to another table's columns where the values for a dimension or argument can
 * queried.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "table",
    "namespace",
    "column",
    "suggestionColumns"
})
@Data
@EqualsAndHashCode()
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TableSource implements Serializable {
    private static final long serialVersionUID = 5721654374755116755L;

    @JsonProperty("table")
    private String table;

    @JsonProperty("namespace")
    private String namespace = DEFAULT;

    @JsonProperty("column")
    private String column;

    @JsonProperty("suggestionColumns")
    @JsonDeserialize(as = LinkedHashSet.class)
    private Set<String> suggestionColumns = new LinkedHashSet<>();
}
