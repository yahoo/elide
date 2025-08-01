/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql;

import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.request.EntityProjection;
import com.yahoo.elide.core.type.Type;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Represents a GraphQL Input Object.
 */
public class Entity {
    @Getter private Optional<Entity> parentResource;
    private Map<String, Object> data;
    @Getter private Type<?> entityClass;
    @Getter private RequestScope requestScope;
    @Getter private Set<Attribute> attributes;
    @Getter private Set<Relationship> relationships;

    /**
     * Class constructor.
     * @param parentResource parent entity
     * @param data entity data
     * @param entityClass entity class
     * @param requestScope the request context object
     */
    public Entity(
            Optional<Entity> parentResource,
            Map<String, Object> data,
            Type<?> entityClass,
            RequestScope requestScope) {
        this.parentResource = parentResource;
        this.data = data;
        this.entityClass = entityClass;
        this.requestScope = requestScope;
        setAttributes();
        setRelationships();
    }

    @AllArgsConstructor
    public class Attribute {
        @Getter private String name;
        @Getter private Object value;
    }

    @AllArgsConstructor
    class Relationship {
        @Getter private String name;
        @Getter private Set<Entity> value;
    }

    /**
     * Extract the attributes of the entity.
     */
    private void setAttributes() {
        if (this.data != null) {
            this.attributes = new LinkedHashSet<>();
            EntityDictionary dictionary = this.requestScope.getDictionary();
            String idFieldName = dictionary.getIdFieldName(this.entityClass);

            for (Map.Entry<String, Object> entry : this.data.entrySet()) {
                if (dictionary.isAttribute(this.entityClass, entry.getKey())) {
                    this.attributes.add(new Attribute(entry.getKey(), entry.getValue()));
                }
                if (Objects.equals(entry.getKey(), idFieldName)) {
                    this.attributes.add(new Attribute(entry.getKey(), entry.getValue()));
                }
            }
        }
    }

    /**
     * Extract the relationships of the entity.
     */
    private void setRelationships() {
        if (this.data != null) {
            this.relationships = new LinkedHashSet<>();
            EntityDictionary dictionary = this.requestScope.getDictionary();

            this.data.entrySet().stream()
                    .filter(entry -> dictionary.isRelation(this.entityClass, entry.getKey()))
                    .forEach(entry -> {
                        String relationshipName = entry.getKey();
                        Type<?> relationshipClass =
                                dictionary.getParameterizedType(this.entityClass, relationshipName);

                        Set<Entity> relationshipEntities = new LinkedHashSet<>();

                        // if the relationship is ToOne, entry.getValue() should be a single map
                        if (dictionary.getRelationshipType(this.entityClass, relationshipName).isToOne()) {
                            relationshipEntities.add(new Entity(
                                    Optional.of(this),
                                    ((Map<String, Object>) entry.getValue()),
                                    relationshipClass,
                                    this.requestScope));
                        } else {
                            for (Map<String, Object> row : (List<Map<String, Object>>) entry.getValue()) {
                                relationshipEntities.add(new Entity(
                                        Optional.of(this),
                                        row,
                                        relationshipClass,
                                        this.requestScope));
                            }
                        }
                        this.relationships.add(new Relationship(relationshipName, relationshipEntities));
                    });
        }
    }

    /**
     * Get the relationship with name.
     * @param name Name of relationship
     * @return Relationship
     */
    public Optional<Relationship> getRelationship(String name) {
        Iterator<Relationship> it = this.relationships.iterator();
        while (it.hasNext()) {
            Relationship relationship = it.next();
            if (relationship.getName().equals(name)) {
                return Optional.of(relationship);
            }
        }
        return Optional.empty();
    }

    /**
     * Get an Attribute with name.
     * @param name Name of attribute
     * @return Attribute
     */
    public Optional<Attribute> getAttribute(String name) {
        Iterator<Attribute> it = this.attributes.iterator();
        while (it.hasNext()) {
            Attribute attribute = it.next();
            if (attribute.getName().equals(name)) {
                return Optional.of(attribute);
            }
        }
        return Optional.empty();
    }

    /**
     * Get the id of the entity.
     * @return the optional id
     */
    public Optional<String> getId() {
        EntityDictionary dictionary = this.requestScope.getDictionary();
        String idFieldName = dictionary.getIdFieldName(this.entityClass);
        if (this.attributes == null) {
            return Optional.empty();
        }
        return this.attributes.stream()
                .filter(entry -> idFieldName.equalsIgnoreCase(entry.name))
                .map(e -> (String) e.value)
                .findFirst();
    }

    /**
     * Set the id of the entity if it doesn't have one already.
     */
    public void setId() {
        if (!getId().isPresent()) {
            EntityDictionary dictionary = this.requestScope.getDictionary();
            String idFieldName = dictionary.getIdFieldName(this.entityClass);
            String uuid = UUID.randomUUID().toString();
            this.attributes.add(new Attribute(idFieldName, uuid));
        }
    }

    /**
     * Convert {@link Entity} to {@link PersistentResource} object.
     * @return {@link PersistentResource} object
     */
    public PersistentResource toPersistentResource() {
        return this.data == null
                ? null
                : PersistentResource.loadRecord(getProjection(), getId().orElse(null), this.requestScope);
    }

    /**
     * Get a projection for this entity class. Used for querying inserted entities.
     * @return {@link EntityProjection} object
     */
    public EntityProjection getProjection() {
        return EntityProjection.builder().type(entityClass).build();
    }
}
