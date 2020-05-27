/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.request;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

/**
 * Represents a relationship on an Elide entity.
 */
@Data
@Builder
public class Relationship {

    @NonNull
    private String name;

    private String alias;

    //If null, the parentType is the same as the entity projection to which this relationship belongs.
    //If not null, this represents the model type where this relationship can be found.
    private Class<?> parentType;

    @NonNull
    private EntityProjection projection;

    private Relationship(@NonNull String name, String alias, @NonNull EntityProjection projection) {
        this.name = name;
        this.parentType = null;
        this.alias = alias == null ? name : alias;
        this.projection = projection;
    }

    private Relationship(@NonNull String name, String alias, Class<?> parentType,
                         @NonNull EntityProjection projection) {
        this.name = name;
        this.parentType = parentType;
        this.alias = alias == null ? name : alias;
        this.projection = projection;
    }

    public RelationshipBuilder copyOf() {
        return Relationship.builder()
                .alias(alias)
                .name(name)
                .projection(projection);
    }

    public Relationship merge(Relationship toMerge) {
        return Relationship.builder()
                .name(name)
                .alias(alias)
                .projection(projection.merge(toMerge.projection))
                .build();
    }
}
