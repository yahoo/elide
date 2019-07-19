/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.request;

import com.yahoo.elide.core.EntityDictionary;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.Singular;

import java.util.Set;

/**
 * Represents a client data request against a subgraph of the entity relationship graph.
 */
@Data
@Builder
public class DataCollection {
    @NonNull
    private EntityDictionary dictionary;

    private DataCollection parent;

    @NonNull
    private Class<?> type;

    @Singular
    private Set<Attribute> attributes;

    @Singular
    private Set<DataCollection> relationships;

    /**
     * Returns the entity name.
     * @return the entity name
     */
    public String getName() {
        return dictionary.getJsonAliasFor(type);
    }

    /**
     * Returns a relationship subgraph by name.
     * @param name The name of the relationship.
     * @return
     */
    public DataCollection getDataCollection(String name) {
        return relationships.stream()
                .filter((relationship) -> name.equals(relationship.getName()))
                .findFirst()
                .orElse(null);
    }
}
