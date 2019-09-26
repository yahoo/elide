/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.engine;

import com.yahoo.elide.core.EntityDictionary;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * An auxiliary class for {@link AbstractEntityHydrator} and is responsible for setting relationship values of an entity
 * instance.
 * <p>
 * {@link StitchList} should not be subclassed.
 * <p>
 * Concurrency note: {@link #stitch()} operation generally do not block, so may overlap with update operations (
 * {@link #todo(Object, String, Object)} and {@link #populateLookup(Class, Map)}). {@link #stitch()} reflects the
 * results of the most recently completed update operations holding upon their onset. (More formally, an update
 * operation bears a happens-before relation with a {@link #stitch()} operation.)
 */
public final class StitchList {

    /**
     * A representation of an TODO item in a {@link StitchList}.
     */
    @Data
    public static class Todo {
        private final Object entityInstance;
        private final String relationshipName;
        private final Object foreignKey;
    }

    /**
     * Maps an relationship entity class to a map of object ID to object instance.
     * <p>
     * For example, [Country.class: [340: Country(id:340), 100: Country(id:100)]]
     */
    @Getter(AccessLevel.PRIVATE)
    private final Map<Class<?>, Map<Object, Object>> objectLookups;

    /**
     * List of relationships to hydrate
     */
    @Getter(AccessLevel.PRIVATE)
    private final List<Todo> todoList;

    @Getter(AccessLevel.PRIVATE)
    private final EntityDictionary entityDictionary;

    /**
     * Constructor.
     *
     * @param entityDictionary  An object that sets entity instance values and provides entity metadata info
     */
    public StitchList(EntityDictionary entityDictionary) {
        this.objectLookups = new ConcurrentHashMap<>();
        this.todoList = Collections.synchronizedList(new ArrayList<>());
        this.entityDictionary = entityDictionary;
    }

    /**
     * Returns whether or not the entity instances in this {@link StitchList} have relationships that are unset.
     *
     * @return {@code true} if the entity instances in this {@link StitchList} should be further hydrated because they
     * have one or more relationship fields.
     */
    public boolean shouldStitch() {
        return !getTodoList().isEmpty();
    }

    /**
     * Enqueues an entity instance which will be further hydrated on one of its relationship fields later
     *
     * @param entityInstance  The entity instance to be hydrated
     * @param fieldName  The relationship field to hydrate in the entity instance
     * @param value  The foreign key between the entity instance and the field entity.
     */
    public void todo(Object entityInstance, String fieldName, Object value) {
        getTodoList().add(new Todo(entityInstance, fieldName, value));
    }

    /**
     * Sets all the relationship values of an requested entity.
     * <p>
     * Values associated with the existing key will be overwritten.
     *
     * @param relationshipType  The type of the relationship to set
     * @param idToInstance  A map from relationship ID to the actual relationship instance with that ID
     */
    public void populateLookup(Class<?> relationshipType, Map<Object, Object> idToInstance) {
        getObjectLookups().put(relationshipType, idToInstance);
    }

    /**
     * Stitch all entity instances currently in this {@link StitchList} by setting their relationship fields whose
     * values are determined by relationship ID's.
     */
    public void stitch() {
        for (Todo todo : getTodoList()) {
            Object entityInstance = todo.getEntityInstance();
            String relationshipName = todo.getRelationshipName();
            Object foreignKey = todo.getForeignKey();

            Class<?> relationshipType = getEntityDictionary().getType(entityInstance, relationshipName);
            Object relationshipValue = getObjectLookups().get(relationshipType).get(foreignKey);

            getEntityDictionary().setValue(entityInstance, relationshipName, relationshipValue);
        }
    }

    /**
     * Returns a mapping from relationship name to an immutable list of foreign key ID objects.
     * <p>
     * For example, given the following {@code todoList}:
     * <pre>
     * {@code
     *     [PlayerStats, country, 344]
     *     [PlayerStats, country, 840]
     *     [PlayerStats, country, 344]
     *     [PlayerStats, player, 1]
     *     [PlayerStats, player, 1]
     *     [PlayerStats, player, 1]
     * }
     * </pre>
     * this method returns a map of the following:
     * <pre>
     *     [
     *         "country": [344, 840]
     *         "player": [1]
     *     ]
     * </pre>
     *
     * @return a mapping from relationship name to an ordered list of relationship join ID's
     */
    public Map<String, List<Object>> getHydrationMapping() {
        return getTodoList().stream()
                .collect(
                        Collectors.groupingBy(
                                Todo::getRelationshipName,
                                Collectors.mapping(
                                        Todo::getForeignKey,
                                        Collectors.collectingAndThen(
                                                Collectors.toCollection(LinkedList::new),
                                                Collections::unmodifiableList
                                        )
                                )
                        )
                );
    }
}
