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

    public RelationshipBuilder copyOf() {
        return Relationship.builder()
                .alias(alias)
                .name(name)
                .projection(projection);
    }

    @NonNull
    private String name;

    private String alias;

    @NonNull
    private EntityProjection projection;

    public Relationship merge(Relationship toMerge) {
        return Relationship.builder()
                .name(name)
                .alias(alias)
                .projection(projection.merge(toMerge.projection))
                .build();
    }
}
