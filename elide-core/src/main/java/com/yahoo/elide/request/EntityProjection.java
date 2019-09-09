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

import java.util.HashSet;
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
    private Set<Relationship> relationships;

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
    public EntityProjectionBuilder copyOf() {
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
    public Relationship getRelationship(String name) {
        return relationships.stream()
                .filter((relationship) -> relationship.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    /**
     * Returns a relationship subgraph by name.
     * @param name The name of the relationship.
     * @param name The alias of the relationship.
     * @return
     */
    public Relationship getRelationship(String name, String alias) {
        return relationships.stream()
                .filter((relationship) -> relationship.getName().equalsIgnoreCase(name))
                .filter((relationship) -> relationship.getAlias().equalsIgnoreCase(alias))
                .findFirst()
                .orElse(null);
    }

    /**
     * Recursively merges two EntityProjections.
     * @param toMerge The projection to merge
     * @return A newly created and merged EntityProjection.
     */
    public EntityProjection merge(EntityProjection toMerge) {
        EntityProjectionBuilder merged = copyOf();

        for (Relationship relationship: toMerge.getRelationships()) {
            EntityProjection theirs = relationship.getProjection();
            EntityProjection ours = getRelationship(relationship.getName(), relationship.getAlias()).getProjection();

            if (ours != null) {
                merged.relationship(Relationship.builder()
                        .name(relationship.getName())
                        .alias(relationship.getAlias())
                        .projection(ours.merge(theirs))
                        .build());
            } else {
                merged.relationship(relationship);
            }
        }
        merged.attributes.addAll(toMerge.attributes);

        return merged.build();
    }

    /**
     * Customizes the lombok builder to our needs.
     */
    public static class EntityProjectionBuilder {
        private Set<Relationship> relationships = new HashSet<>();

        public EntityProjectionBuilder relationships(Set<Relationship> relationships) {
            this.relationships = relationships;
            return this;
        }

        public EntityProjectionBuilder relationship(String name, EntityProjection projection) {
            relationships.add(Relationship.builder()
                    .alias(name)
                    .name(name)
                    .projection(projection)
                    .build());
            return this;
        }

        public EntityProjectionBuilder relationship(Relationship relationship) {
            Relationship existing = relationships.stream()
                    .filter(r -> r.getName().equals(relationship.getName()))
                    .filter(r -> r.getAlias().equals(relationship.getAlias()))
                    .findFirst().orElse(null);

            if (existing != null) {
                relationships.add(Relationship.builder()
                        .alias(existing.getAlias())
                        .name(existing.getName())
                        .projection(existing.getProjection().merge(relationship.getProjection()))
                        .build());
            } else {
                relationships.add(relationship);
            }
            return this;
        }
    }
}
