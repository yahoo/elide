/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.request;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.pagination.Pagination;
import com.yahoo.elide.core.sort.Sorting;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.Singular;

import java.util.Map;
import java.util.Set;

/**
 * Represents a client data request against a subgraph of the entity relationship graph.
 */
@Data
@Builder
@AllArgsConstructor
public class EntityProjection {
    @NonNull
    private EntityDictionary dictionary;

    @NonNull
    private Class<?> type;

    @Singular
    private Set<Attribute> attributes;

    @Singular
    private Map<String, EntityProjection> relationships;

    private FilterExpression filterExpression;

    private Sorting sorting;

    private Pagination pagination;

    /**
     * Returns the entity name.
     * @return the entity name
     */
    public String getName() {
        return dictionary.getJsonAliasFor(type);
    }

    /**
     * Creates a builder initialized as a copy of this collection
     * @return The new builder
     */
    public EntityProjectionBuilder withProjection() {
        return EntityProjection.builder()
                .dictionary(this.dictionary)
                .type(this.type)
                .attributes(this.attributes)
                .relationships(this.relationships)
                .filterExpression(this.filterExpression)
                .sorting(this.sorting)
                .pagination(this.pagination);
    }

    /**
     * Returns a relationship subgraph by name.
     * @param name The name of the relationship.
     * @return
     */
    public EntityProjection getRelationship(String name) {
        return relationships.get(name);
    }

    /**
     * Recursively merges two EntityProjections by their relationships.
     * @param toMerge The projection to merge
     * @return A newly created & merged EntityProjection.
     */
    public EntityProjection mergeRelationships(EntityProjection toMerge) {
        EntityProjectionBuilder merged = withProjection();

        for (Map.Entry<String, EntityProjection> entry : toMerge.getRelationships().entrySet()) {
            String relationshipName = entry.getKey();

            EntityProjection theirs = entry.getValue();
            EntityProjection ours = relationships.get(relationshipName);

            if (ours != null) {
                merged.relationship(relationshipName, ours.mergeRelationships(theirs));
            } else {
                merged.relationship(relationshipName, theirs);
            }
        }

        return merged.build();
    }
}
