/* Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.RequestScope;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class Entity {
    private Optional<Entity> parentResource;
    private Optional<Map<String, Object>> data;
    private Class<?> entityClass;

    /**
     * Class constructor
     * @param parentResource parent entity
     * @param data entity data
     * @param entityClass binding entity class
     */
    protected Entity(Optional<Entity> parentResource, Optional<Map<String, Object>> data, Class<?> entityClass) {
        this.parentResource = parentResource;
        this.data = data;
        this.entityClass = entityClass;
    }

    /**
     * Getter for binding entity class
     */
    public Class<?> getEntityClass() {
        return this.entityClass;
    }

    /**
     * Getter for entity data
     */
    public Map<String, Object> getData() {
        return this.data.orElse(null);
    }

    /**
     * Getter for parent entity
     */
    public Optional<Entity> getParentResource() {
        return parentResource;
    }

    /**
     * Get the id of the entity
     * @param requestScope Request scope
     * @return the optional id
     */
    public Optional<String> getId(RequestScope requestScope) {
        EntityDictionary dictionary = requestScope.getDictionary();
        if(this.data.isPresent()) {
            String idFieldName = dictionary.getIdFieldName(this.entityClass);
            return this.data.get().entrySet().stream()
                    .filter(entry -> idFieldName.equalsIgnoreCase(entry.getKey()))
                    .map(e -> (String) e.getValue())
                    .findFirst();
        } else {
            return Optional.empty();
        }
    }

    /**
     * Set the id of the entity if it doesn't have one already
     * @param requestScope Request scope
     */
    public void setId(RequestScope requestScope) {
        EntityDictionary dictionary = requestScope.getDictionary();
        String idFieldName = dictionary.getIdFieldName(this.entityClass);
        if(!getId(requestScope).isPresent() && this.data.isPresent()) {
            String uuid = UUID.randomUUID().toString()
                    .replaceAll("[^0-9]", "")
                    .substring(0, 3); //limit the number of digits to prevent InvalidValueException in PersistentResource.createObject()
            //TODO: this is hacky, ask for a workaround for this.
            this.data.get().put(idFieldName, uuid);
        }
    }

    /**
     * Convert {@link Entity} to {@link PersistentResource} object
     * @param requestScope the request scope
     * @return {@link PersistentResource} object
     */
    public PersistentResource toPersistentResource(RequestScope requestScope) {
        return PersistentResource.loadRecord(this.entityClass, getId(requestScope).orElse(null), requestScope);
    }

    /**
     * Strips out the attributes from the {@code element} singleton list and returns just the relationships, if present.
     * @param requestScope Request scope
     * @return relationship map
     */
    public Set<Entity> stripAttributes(RequestScope requestScope) {
        EntityDictionary dictionary = requestScope.getDictionary();
        Set<Entity> relationshipEntities = new HashSet();
        Map<String, Object> relationship = new HashMap<>();
        Optional<Map<String, Object>> element = this.data;

        if(!element.isPresent()) {
            return Collections.emptySet();
        }

        for(Map.Entry<String, Object> entry : element.get().entrySet()) {
            if(dictionary.isRelation(this.entityClass, entry.getKey())) {
                relationship.put(entry.getKey(), entry.getValue());
                Class<?> loadClass = dictionary.getParameterizedType(this.entityClass, entry.getKey());
                relationshipEntities.add(new Entity(Optional.of(this), Optional.of(relationship), loadClass));
            }
        }
        return relationshipEntities;
    }

    /**
     * Strips out the relationships from the {@code element} singleton list and returns just the attributes, if present.
     * @param requestScope Request scope
     * @return attributes map
     */
    public Entity stripRelationships(RequestScope requestScope) {
        EntityDictionary dictionary = requestScope.getDictionary();
        Map<String, Object> attributesOnly = new HashMap<>();
        String idFieldName = dictionary.getIdFieldName(this.entityClass);
        Optional<Map<String, Object>> element = this.data;

        if(!element.isPresent()) {
            return null;
        }

        for(Map.Entry<String, Object> entry : element.get().entrySet()) {
            if(dictionary.isAttribute(this.entityClass, entry.getKey())) {
                attributesOnly.put(entry.getKey(), entry.getValue());
            }
            if(Objects.equals(entry.getKey(), idFieldName)) {
                attributesOnly.put(entry.getKey(), entry.getValue());
            }
        }
        return new Entity(this.parentResource, Optional.ofNullable(attributesOnly), this.entityClass);
    }
}