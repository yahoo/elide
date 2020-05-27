/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.request;

import com.yahoo.elide.core.exceptions.BadRequestException;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.google.common.collect.Sets;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
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
        @Getter
        private Class<?> type;

        private Set<Relationship> relationships = new LinkedHashSet<>();

        private Set<Attribute> attributes = new LinkedHashSet<>();

        @Getter
        private FilterExpression filterExpression;

        @Getter
        private Sorting sorting;

        @Getter
        private Pagination pagination;

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
                    .parentType(projection.type)
                    .name(name)
                    .projection(projection)
                    .build());
        }

        /**
         * Add a new relationship into this project or merge an existing relationship that has same field name
         * and alias as this relationship. If there exists another attribute/relationship of different field that is
         * using the same alias, it would throw exception because that's ambiguous.
         *
         * @param relationship new relationship to add
         * @return this builder after adding the relationship
         */
        public EntityProjectionBuilder relationship(Relationship relationship) {
            String relationshipName = relationship.getName();
            String relationshipAlias = relationship.getAlias();

            Relationship existing = relationships.stream()
                    .filter(r -> r.getName().equals(relationshipName) && r.getAlias().equals(relationshipAlias))
                    .findFirst().orElse(null);

            if (existing != null) {
                relationships.remove(existing);
                relationships.add(Relationship.builder()
                        .parentType(relationship.getParentType())
                        .name(relationshipName)
                        .alias(relationshipAlias)
                        .projection(existing.getProjection().merge(relationship.getProjection()))
                        .build());
            } else {
                if (isAmbiguous(relationshipName, relationshipAlias)) {
                    throw new BadRequestException(
                            String.format("Alias {%s}.{%s} is ambiguous.", type, relationshipAlias)
                    );
                }
                relationships.add(relationship);
            }

            return this;
        }

        /**
         * Add a new attribute into this project or merge an existing attribute that has same field name
         * and alias as this attribute. If there exists another attribute/relationship of different field that is
         * using the same alias, it would throw exception because that's ambiguous.
         *
         * @param attribute new attribute to add
         * @return this builder after adding the attribute
         */
        public EntityProjectionBuilder attribute(Attribute attribute) {
            String attributeName = attribute.getName();
            String attributeAlias = attribute.getAlias();

            Attribute existing = attributes.stream()
                    .filter(a -> a.getName().equals(attributeName) && a.getAlias().equals(attributeAlias))
                    .findFirst().orElse(null);

            if (existing != null) {
                attributes.remove(existing);
                attributes.add(Attribute.builder()
                        .type(attribute.getType())
                        .parentType(attribute.getParentType())
                        .name(attributeName)
                        .alias(attributeAlias)
                        .arguments(Sets.union(attribute.getArguments(), existing.getArguments()))
                        .build());
            } else {
                if (isAmbiguous(attributeName, attributeAlias)) {
                    throw new BadRequestException(
                            String.format("Alias {%s}.{%s} is ambiguous.", type, attributeAlias)
                    );
                }
                attributes.add(attribute);
            }

            return this;
        }

        /**
         * Get an attribute by alias.
         *
         * @param attributeAlias alias to refer to an attribute field
         * @return found attribute or null
         */
        public Attribute getAttributeByAlias(String attributeAlias) {
            return attributes.stream()
                    .filter(attribute -> attribute.getAlias().equals(attributeAlias))
                    .findAny()
                    .orElse(null);
        }

        /**
         * Get an relationship by alias.
         *
         * @param relationshipAlias alias to refer to a relationship field
         * @return found attribute or null
         */
        public Relationship getRelationshipByAlias(String relationshipAlias) {
            return relationships.stream()
                    .filter(relationship -> relationship.getAlias().equals(relationshipAlias))
                    .findAny()
                    .orElse(null);
        }

        /**
         * Check whether a field alias is ambiguous.
         *
         * @param fieldName field that the alias is bound to
         * @param alias an field alias
         * @return whether new alias would cause ambiguous
         */
        private boolean isAmbiguous(String fieldName, String alias) {
            return attributes.stream().anyMatch(a -> !fieldName.equals(a.getName()) && alias.equals(a.getAlias()))
                    || relationships.stream().anyMatch(
                            r -> !fieldName.equals(r.getName()) && alias.equals(r.getAlias()));
        }
    }
}
