/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.request;

import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.pagination.Pagination;
import com.yahoo.elide.core.sort.Sorting;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Represents a client data request against a subgraph of the entity relationship graph.
 */
@Data
@Builder
@AllArgsConstructor
public class EntityProjection {
    @NonNull
    private Class<?> type;

    private Set<Attribute> attributes;

    private Set<Relationship> relationships;

    private FilterExpression filterExpression;

    private Sorting sorting;

    private Pagination pagination;

    /**
     * Creates a builder initialized as a copy of this collection
     * @return The new builder
     */
    public EntityProjectionBuilder copyOf() {
        return EntityProjection.builder()
                .type(this.type)
                .attributes(new LinkedHashSet<>(attributes))
                .relationships(new LinkedHashSet<>(this.relationships))
                .filterExpression(this.filterExpression)
                .sorting(this.sorting)
                .pagination(this.pagination);
    }

    /**
     * Returns a relationship subgraph by name.
     * @param name The name of the relationship.
     * @return
     */
    public Optional<Relationship> getRelationship(String name) {
        return relationships.stream()
                .filter((relationship) -> relationship.getName().equalsIgnoreCase(name))
                .findFirst();
    }

    /**
     * Returns a relationship subgraph by name.
     * @param name The name of the relationship.
     * @param name The alias of the relationship.
     * @return
     */
    public Optional<Relationship> getRelationship(String name, String alias) {
        return relationships.stream()
                .filter((relationship) -> relationship.getName().equalsIgnoreCase(name))
                .filter((relationship) -> relationship.getAlias().equalsIgnoreCase(alias))
                .findFirst();
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

            Relationship ourRelationship =  getRelationship(relationship.getName(),
                    relationship.getAlias()).orElse(null);

            if (ourRelationship != null) {
                merged.relationships.remove(ourRelationship);
                merged.relationships.add((Relationship.builder()
                        .name(relationship.getName())
                        .alias(relationship.getAlias())
                        .projection(ourRelationship.getProjection().merge(theirs))
                        .build()));
            } else {
                merged.relationships.add((relationship));
            }
        }
        if (toMerge.getPagination() != null) {
            merged.pagination = toMerge.getPagination();
        }

        if (toMerge.getSorting() != null) {
            merged.sorting = toMerge.getSorting();
        }

        if (toMerge.getFilterExpression() != null) {
            merged.filterExpression = toMerge.getFilterExpression();
        }

        merged.attributes.addAll(toMerge.attributes);

        return merged.build();
    }

    /**
     * Customizes the lombok builder to our needs.
     */
    public static class EntityProjectionBuilder {
        private Set<Relationship> relationships = new LinkedHashSet<>();
        private Set<Attribute> attributes = new LinkedHashSet<>();

        public EntityProjectionBuilder relationships(Set<Relationship> relationships) {
            this.relationships = relationships;
            return this;
        }

        public EntityProjectionBuilder attributes(Set<Attribute> attributes) {
            this.attributes = attributes;
            return this;
        }

        public EntityProjectionBuilder relationship(String name, EntityProjection projection) {
            return relationship(Relationship.builder()
                    .alias(name)
                    .name(name)
                    .projection(projection)
                    .build());
        }

        public EntityProjectionBuilder relationship(Relationship relationship) {
            Relationship existing = relationships.stream()
                    .filter(r -> r.getName().equals(relationship.getName()))
                    .filter(r -> r.getAlias().equals(relationship.getAlias()))
                    .findFirst().orElse(null);

            if (existing != null) {
                relationships.remove(existing);
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

        public EntityProjectionBuilder attribute(Attribute attribute) {
            this.attributes.add(attribute);
            return this;
        }
    }

    /**
     * Get an attribute by name.
     *
     * @param attributeName attribute name to get
     * @return found attribute or null
     */
    public Attribute getAttributeByName(String attributeName) {
        return getAttributes().stream()
                .filter(attribute -> attribute.getName().equals(attributeName))
                .findAny()
                .orElse(null);
    }
}
