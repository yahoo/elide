package com.yahoo.elide.datastores.aggregation.engine;

import com.yahoo.elide.core.EntityDictionary;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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
     * A representation of an element in a {@link StitchList}.
     */
    @Data
    public static class Todo {
        private final Object entityInstance;
        private final String relationshipName;
        private final Object foreignKey;
    }

    /**
     * Maps an relationship entity class to a Map of object ID to object instance.
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

    public StitchList(EntityDictionary entityDictionary) {
        this.objectLookups = new ConcurrentHashMap<>();
        this.todoList = Collections.synchronizedList(new ArrayList<>());
        this.entityDictionary = entityDictionary;
    }

    public boolean shouldStitch() {
        return !getTodoList().isEmpty();
    }

    public void todo(Object entityInstance, String fieldName, Object value) {
        getTodoList().add(new Todo(entityInstance, fieldName, value));
    }

    /**
     * Any existing values will be overwritten.
     *
     * @param relationshipType
     * @param idToInstance
     */
    public void populateLookup(Class<?> relationshipType, Map<Object, Object> idToInstance) {
        getObjectLookups().put(relationshipType, idToInstance);
    }

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
                                StitchList.Todo::getRelationshipName,
                                Collectors.mapping(StitchList.Todo::getForeignKey, Collectors.toCollection(LinkedList::new))
                        )
                );
    }
}
